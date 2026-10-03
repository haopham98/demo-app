# Example configuration variables for Terraform
aws_region  = "ap-southeast-1"
environment = "dev"
# db_password is provided securely via AWS SSM (/demo/dev/variables) or TF_VAR_db_password
# db_password = "ChangeThisStrongPassword123!"

# CIDR customization (optional)
vpc_cidr              = "10.0.0.0/16"
public_subnet_cidr    = "10.0.1.0/24"
private_subnet_1_cidr = "10.0.10.0/24"
private_subnet_2_cidr = "10.0.20.0/24"

# Compute instance types
instance_type     = "t3.micro"
db_instance_class = "db.t3.micro"
