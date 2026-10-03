# Rules for IaC and Pipeline Generation
- Always use Terraform >= 1.5.0 with modular structure (main.tf, variables.tf, outputs.tf, provider.tf).
- Target AWS Free-Tier resources (t3.micro, 20GB RDS) unless specified otherwise.
- Store Terraform State remotely using S3 and native S3 lockfile (`use_lockfile = true`).
- Generate a declarative Jenkinsfile with credentials binding for AWS keys.
- Generate comment in English