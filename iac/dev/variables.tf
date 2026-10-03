variable "aws_region" {
  type        = string
  description = "AWS region to deploy resources"
  default     = "ap-southeast-1"
}

variable "environment" {
  type        = string
  description = "Deployment environment (e.g., non-prod, prod)"
  default     = "non-prod"
}

variable "db_password" {
  type        = string
  description = "Master password for RDS PostgreSQL instance"
  sensitive   = true
}

variable "vpc_cidr" {
  type        = string
  description = "CIDR block for the VPC"
  default     = "10.0.0.0/16"
}

variable "public_subnet_cidr" {
  type        = string
  description = "CIDR block for the Public Subnet"
  default     = "10.0.1.0/24"
}

variable "private_subnet_1_cidr" {
  type        = string
  description = "CIDR block for Private Subnet 1 (AZ a)"
  default     = "10.0.10.0/24"
}

variable "private_subnet_2_cidr" {
  type        = string
  description = "CIDR block for Private Subnet 2 (AZ b)"
  default     = "10.0.20.0/24"
}

variable "instance_type" {
  type        = string
  description = "EC2 instance type"
  default     = "t3.micro"
}

variable "instance_count" {
  type        = number
  description = "Number of EC2 instances to deploy"
  default     = 2
}

variable "db_instance_class" {
  type        = string
  description = "RDS instance class"
  default     = "db.t3.micro"
}
