# VPC Outputs
output "vpc_id" {
  description = "The ID of the VPC"
  value       = module.vpc.vpc_id
}

output "public_subnet_id" {
  description = "The ID of the public subnet"
  value       = module.vpc.public_subnet_id
}

output "private_subnet_ids" {
  description = "The IDs of the private subnets"
  value       = module.vpc.private_subnet_ids
}

# EC2 Outputs
output "ec2_instance_ids" {
  description = "The IDs of the EC2 instances"
  value       = module.ec2.instance_ids
}

output "ec2_public_ips" {
  description = "The public IP addresses of the EC2 instances"
  value       = module.ec2.public_ips
}

output "ec2_security_group_id" {
  description = "The security group ID of the EC2 instances"
  value       = module.ec2.security_group_id
}

# RDS Outputs
output "rds_endpoint" {
  description = "The connection endpoint for the RDS instance"
  value       = module.rds.db_endpoint
}

output "rds_address" {
  description = "The hostname of the RDS instance"
  value       = module.rds.db_address
}

output "rds_port" {
  description = "The database port"
  value       = module.rds.db_port
}
