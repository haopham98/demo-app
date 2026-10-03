pipeline {
    agent any

    options {
        // Prevent concurrent builds of the same job to avoid conflicts in Terraform state
        disableConcurrentBuilds()
        timestamps()
        ansiColor('xterm')
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
        TF_DIR             = '${params.ENVIRONMENT}'
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
                withCredentials([usernamePassword(credentialsId: 'github-credentials-id', usernameVariable: 'GIT_USERNAME', passwordVariable: 'GIT_PASSWORD')]) {
                    checkout([$class: 'GitSCM',
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
                    // Select AWS credentials based on the environment
                    def awsCredsId = (params.ENVIRONMENT == 'prod') ? 'aws-prod-credentials-id' : 'aws-dev-credentials-id'
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
                    def awsCredsId = (params.ENVIRONMENT == 'prod') ? 'aws-prod-credentials-id' : 'aws-dev-credentials-id'

                    withCredentials([
                        usernamePassword(
                            credentialsId: awsCredsId,
                            usernameVariable: 'AWS_ACCESS_KEY_ID',
                            passwordVariable: 'AWS_SECRET_ACCESS_KEY'
                        )
                    ]) {
                        dir(env.TF_DIR) {
                            echo "--> Execute Terraform Plan for environment ${params.ENVIRONMENT}..."

                            // Create plan file (support both apply and destroy)
                            if (params.ACTION == 'destroy') {
                                sh "terraform plan -destroy -var='environment=${params.ENVIRONMENT}' -out=tfplan"
                            } else {
                                sh "terraform plan -var='environment=${params.ENVIRONMENT}' -out=tfplan"
                            }
                        }
                    }
                }
            }
        }

        stage('Manual Approval') {
            when {
                // For Prod deployments, require manual approval before proceeding to apply
                expression { params.ENVIRONMENT == 'prod' }
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
                    def awsCredsId = (params.ENVIRONMENT == 'prod') ? 'aws-prod-credentials-id' : 'aws-dev-credentials-id'

                    withCredentials([
                        usernamePassword(
                            credentialsId: awsCredsId,
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
