variable "environment" {
  type        = string
  description = "Deployment environment (e.g. dev, non-prod, prod)"
}

variable "vpc_id" {
  type        = string
  description = "The VPC ID where the EC2 instances and security group will be created"
}

variable "subnet_id" {
  type        = string
  description = "The Subnet ID in which to launch the EC2 instances"
}

variable "instance_type" {
  type        = string
  description = "EC2 instance type"
  default     = "t3.micro"
}

variable "instance_count" {
  type        = number
  description = "Number of EC2 instances to launch"
  default     = 2
}

variable "ami_id" {
  type        = string
  description = "Custom AMI ID. If empty, latest Ubuntu 22.04 LTS AMI is resolved automatically."
  default     = ""
}

variable "ssh_allowed_cidrs" {
  type        = list(string)
  description = "CIDR blocks allowed for SSH access"
  default     = ["0.0.0.0/0"]
}

variable "role_tag" {
  type        = string
  description = "Role tag value for the EC2 instances"
  default     = "WebServer"
}
