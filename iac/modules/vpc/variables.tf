variable "environment" {
  type        = string
  description = "Deployment environment (e.g. dev, non-prod, prod)"
}

variable "aws_region" {
  type        = string
  description = "AWS region to deploy subnets in"
  default     = "ap-southeast-1"
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
