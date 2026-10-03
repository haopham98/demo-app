variable "environment" {
  type        = string
  description = "Deployment environment (e.g. dev, non-prod, prod)"
}

variable "vpc_id" {
  type        = string
  description = "The VPC ID where the RDS security group will be created"
}

variable "subnet_ids" {
  type        = list(string)
  description = "List of subnet IDs for the RDS DB subnet group (minimum 2 across different AZs)"
}

variable "db_instance_class" {
  type        = string
  description = "RDS instance class"
  default     = "db.t3.micro"
}

variable "allocated_storage" {
  type        = number
  description = "Allocated storage in gigabytes"
  default     = 20
}

variable "max_allocated_storage" {
  type        = number
  description = "Maximum storage limit in gigabytes for autoscaling"
  default     = 20
}

variable "storage_type" {
  type        = string
  description = "Storage type for the RDS instance"
  default     = "gp2"
}

variable "engine" {
  type        = string
  description = "Database engine"
  default     = "postgres"
}

variable "engine_version" {
  type        = string
  description = "Database engine version"
  default     = "15"
}

variable "db_name" {
  type        = string
  description = "Database name"
  default     = "appdb"
}

variable "db_username" {
  type        = string
  description = "Master username for the database"
  default     = "dbadmin"
}

variable "db_password" {
  type        = string
  description = "Master password for the database"
  sensitive   = true
}

variable "port" {
  type        = number
  description = "The port on which the database accepts connections"
  default     = 5432
}

variable "multi_az" {
  type        = bool
  description = "Specifies if the RDS instance is multi-AZ"
  default     = false
}

variable "publicly_accessible" {
  type        = bool
  description = "Bool to control if instance is publicly accessible"
  default     = false
}

variable "skip_final_snapshot" {
  type        = bool
  description = "Determines whether a final DB snapshot is created before the DB instance is deleted"
  default     = true
}

variable "allowed_security_group_ids" {
  type        = list(string)
  description = "List of Security Group IDs allowed to connect to PostgreSQL"
  default     = []
}

variable "allowed_cidr_blocks" {
  type        = list(string)
  description = "List of CIDR blocks allowed to connect to PostgreSQL"
  default     = []
}
