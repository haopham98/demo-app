def dbPassword = ''
def envName = ''
def awsCredsId = ''

pipeline {
    agent any

    options {
        // Prevent concurrent builds of the same job to avoid conflicts in Terraform state
        disableConcurrentBuilds()
        timeout(time: 2, unit: 'HOURS')
    }

    parameters {
        choice(
            name: 'ENVIRONMENT',
            choices: ['iac/dev', 'iac/prod'],
            description: 'Select the environment to deploy (non-prod or prod)'
        )
        choice(
            name: 'ACTION',
            choices: ['plan', 'apply', 'destroy'],
            description: 'Action to perform: plan, apply, or destroy the infrastructure'
        )
    }

    environment {
        TF_DIR             = "${params.ENVIRONMENT}"
        AWS_DEFAULT_REGION = 'ap-southeast-1'
    }

    stages {
        stage('Checkout') {
            steps {
                echo "=========================================="
                echo "1. Checkout Source Code"
                echo "Environment: ${params.ENVIRONMENT}"
                echo "Action:      ${params.ACTION}"
                echo "=========================================="
                checkout([
                    $class: 'GitSCM',
                    branches: [[name: '*/main']],
                    doGenerateSubmoduleConfigurations: false,
                    extensions: [[$class: 'CleanBeforeCheckout']],
                    userRemoteConfigs: [[
                        url: 'https://github.com/haopham98/demo-app.git',
                        credentialsId: 'haopham-lab-jenkins-code-pull'
                    ]]
                ])
            }
        }

        stage('Fetch Database Password') {
            steps {
                script {
                    envName = (params.ENVIRONMENT.contains('prod')) ? 'prod' : 'dev'
                    awsCredsId = (envName == 'prod') ? 'aws-prod-credentials-id' : 'aws-dev-credentials-id'

                    echo "=========================================="
                    echo "2. Fetch Database Password from AWS SSM"
                    echo "Environment : ${envName}"
                    echo "SSM Path    : /demo/${envName}/variables"
                    echo "Credentials : ${awsCredsId}"
                    echo "=========================================="

                    withCredentials([
                        usernamePassword(
                            credentialsId: awsCredsId,
                            usernameVariable: 'AWS_ACCESS_KEY_ID',
                            passwordVariable: 'AWS_SECRET_ACCESS_KEY'
                        )
                    ]) {
                        dbPassword = sh(
                            script: """
                                set +x
                                PARAM_NAME="/demo/${envName}/variables"
                                RAW_VAL=\$(aws ssm get-parameter --name "\$PARAM_NAME" --with-decryption --query "Parameter.Value" --output text 2>/dev/null || aws ssm get-parameter --name "/demo/dev/variables" --with-decryption --query "Parameter.Value" --output text 2>/dev/null || true)
                                
                                if [ -z "\$RAW_VAL" ]; then
                                    echo "ERROR: Unable to retrieve SSM parameter \$PARAM_NAME or /demo/dev/variables" >&2
                                    exit 1
                                fi

                                python3 -c "
import json, sys
raw = sys.stdin.read().strip()
try:
    data = json.loads(raw)
    if isinstance(data, dict):
        val = data.get('RDS_DB_PASSWD')
        if not val:
            raise ValueError('Key RDS_DB_PASSWD not found in JSON')
        print(val)
    else:
        print(raw)
except Exception:
    print(raw)
" <<< "\$RAW_VAL"
                            """,
                            returnStdout: true
                        ).trim()

                        if (!dbPassword) {
                            error("Failed to retrieve RDS_DB_PASSWD from AWS SSM (/demo/${envName}/variables)")
                        }
                        echo "--> Successfully retrieved RDS_DB_PASSWD from AWS SSM Parameter Store."
                    }
                }
            }
        }

        stage('Terraform Init') {
            steps {
                script {
                    echo "--> Use AWS Credentials ID: ${awsCredsId} for environment ${params.ENVIRONMENT}"

                    withCredentials([
                        usernamePassword(
                            credentialsId: awsCredsId,
                            usernameVariable: 'AWS_ACCESS_KEY_ID',
                            passwordVariable: 'AWS_SECRET_ACCESS_KEY'
                        )
                    ]) {
                        dir(env.TF_DIR) {
                            sh '''
                                echo "=== Check version of terraform ==="
                                terraform version

                                echo "=== Initialize Terraform with S3 remote state ==="
                                terraform init -reconfigure
                            '''
                        }
                    }
                }
            }
        }

        stage('Terraform Plan') {
            steps {
                script {
                    withCredentials([
                        usernamePassword(
                            credentialsId: awsCredsId,
                            usernameVariable: 'AWS_ACCESS_KEY_ID',
                            passwordVariable: 'AWS_SECRET_ACCESS_KEY'
                        )
                    ]) {
                        withEnv([
                            "TF_VAR_db_password=${dbPassword}",
                            "TF_var_db_password=${dbPassword}"
                        ]) {
                            dir(env.TF_DIR) {
                                echo "--> Execute Terraform Plan for environment ${params.ENVIRONMENT}..."

                                if (params.ACTION == 'destroy') {
                                    sh """
                                        set +x
                                        terraform plan -destroy -var="environment=${envName}" -var="db_password=\${TF_VAR_db_password}" -out=tfplan
                                    """
                                } else {
                                    sh """
                                        set +x
                                        terraform plan -var="environment=${envName}" -var="db_password=\${TF_VAR_db_password}" -out=tfplan
                                    """
                                }
                            }
                        }
                    }
                }
            }
        }

        stage('Manual Approval') {
            when {
                // For Prod deployments, require manual approval before proceeding to apply
                expression { params.ENVIRONMENT.contains('prod') && params.ACTION != 'plan' }
            }
            steps {
                timeout(time: 60, unit: 'MINUTES') {
                    script {
                        def actionName = params.ACTION.toUpperCase()
                        def approvalMessage = """
                        =======================================================
                        Approval Required for ${actionName} in PRODUCTION
                        -------------------------------------------------------
                        Environment : ${params.ENVIRONMENT}
                        Action      : ${actionName}
                        IaC Directory: ${env.TF_DIR}
                        -------------------------------------------------------
                        Please review the log of stage 'Terraform Plan' before approval.
                        =======================================================
                        """.stripIndent()

                        input(
                            id: 'ProdDeploymentApproval',
                            message: approvalMessage,
                            ok: "Approve ${actionName} Production",
                            submitterParameter: 'APPROVER_USER'
                        )

                        echo "--> Approved by ${env.APPROVER_USER ?: 'Authorized User'}"
                    }
                }
            }
        }

        stage('Terraform Apply') {
            when {
                expression { params.ACTION != 'plan' }
            }
            steps {
                script {
                    withCredentials([
                        usernamePassword(
                            credentialsId: awsCredsId,
                            usernameVariable: 'AWS_ACCESS_KEY_ID',
                            passwordVariable: 'AWS_SECRET_ACCESS_KEY'
                        )
                    ]) {
                        withEnv([
                            "TF_VAR_db_password=${dbPassword}",
                            "TF_var_db_password=${dbPassword}"
                        ]) {
                            dir(env.TF_DIR) {
                                echo "--> Executing Terraform Apply for ${params.ENVIRONMENT}..."
                                sh 'terraform apply -auto-approve tfplan'

                                if (params.ACTION != 'destroy') {
                                    echo "=== Outputs after deployment ==="
                                    sh 'terraform output'
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    post {
        always {
            script {
                dir(env.TF_DIR) {
                    // Cleanup: Remove the plan file after apply/destroy to avoid confusion
                    sh 'rm -f tfplan'
                }
            }
        }
        success {
            echo "Pipeline completed successfully for environment ${params.ENVIRONMENT} (Action: ${params.ACTION})!"
        }
        failure {
            echo "Pipeline failed! Please check the detailed error in the console output."
        }
    }
}
