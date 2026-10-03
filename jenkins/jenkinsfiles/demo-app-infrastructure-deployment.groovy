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
        AWS_CREDS_ID       = 'aws-cred-dev-lab'
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

        stage('Terraform Init') {
            steps {
                script {
                    echo "--> Use AWS Credentials ID: ${env.AWS_CREDS_ID} for environment ${params.ENVIRONMENT}"

                    withCredentials([
                        usernamePassword(
                            credentialsId: env.AWS_CREDS_ID,
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
                    def envName = params.ENVIRONMENT.contains('prod') ? 'prod' : 'dev'

                    withCredentials([
                        usernamePassword(
                            credentialsId: env.AWS_CREDS_ID,
                            usernameVariable: 'AWS_ACCESS_KEY_ID',
                            passwordVariable: 'AWS_SECRET_ACCESS_KEY'
                        )
                    ]) {
                        dir(env.TF_DIR) {
                            echo "--> Execute Terraform Plan for environment ${params.ENVIRONMENT}..."

                            // Fetch RDS_DB_PASSWD from AWS SSM (/demo/dev/variables) and pass to TF_VAR_db_password
                            if (params.ACTION == 'destroy') {
                                sh """
                                    set +x
                                    echo "--> Fetching database password from AWS SSM..."
                                    RAW_VAL=\$(aws ssm get-parameter --name "/demo/${envName}/variables" --with-decryption --query "Parameter.Value" --output text 2>/dev/null || aws ssm get-parameter --name "/demo/dev/variables" --with-decryption --query "Parameter.Value" --output text 2>/dev/null || true)
                                    
                                    DB_PASS=\$(python3 -c "
import json, sys
raw = sys.stdin.read().strip()
try:
    data = json.loads(raw)
    print(data.get('RDS_DB_PASSWD', raw) if isinstance(data, dict) else raw)
except Exception:
    print(raw)
" <<< "\$RAW_VAL")

                                    export TF_VAR_db_password="\$DB_PASS"
                                    export TF_var_db_password="\$DB_PASS"
                                    set -x

                                    terraform plan -destroy -var="environment=${envName}" -var="db_password=\$DB_PASS" -out=tfplan
                                """
                            } else {
                                sh """
                                    set +x
                                    echo "--> Fetching database password from AWS SSM..."
                                    RAW_VAL=\$(aws ssm get-parameter --name "/demo/${envName}/variables" --with-decryption --query "Parameter.Value" --output text 2>/dev/null || aws ssm get-parameter --name "/demo/dev/variables" --with-decryption --query "Parameter.Value" --output text 2>/dev/null || true)
                                    
                                    DB_PASS=\$(python3 -c "
import json, sys
raw = sys.stdin.read().strip()
try:
    data = json.loads(raw)
    print(data.get('RDS_DB_PASSWD', raw) if isinstance(data, dict) else raw)
except Exception:
    print(raw)
" <<< "\$RAW_VAL")

                                    export TF_VAR_db_password="\$DB_PASS"
                                    export TF_var_db_password="\$DB_PASS"
                                    set -x

                                    terraform plan -var="environment=${envName}" -var="db_password=\$DB_PASS" -out=tfplan
                                """
                            }
                        }
                    }
                }
            }
        }

        stage('Manual Approval') {
            when {
                // require manual approval before proceeding to apply
                expression { params.ACTION == 'apply' }
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
            steps {
                script {
                    withCredentials([
                        usernamePassword(
                            credentialsId: env.AWS_CREDS_ID,
                            usernameVariable: 'AWS_ACCESS_KEY_ID',
                            passwordVariable: 'AWS_SECRET_ACCESS_KEY'
                        )
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