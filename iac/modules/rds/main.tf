# RDS Security Group
resource "aws_security_group" "rds_sg" {
  name        = "demo-rds-sg-${var.environment}"
  description = "Security Group for RDS instance in Private Subnets"
  vpc_id      = var.vpc_id

  dynamic "ingress" {
    for_each = length(var.allowed_security_group_ids) > 0 ? [1] : []
    content {
      description     = "PostgreSQL from allowed Security Groups"
      from_port       = var.port
      to_port         = var.port
      protocol        = "tcp"
      security_groups = var.allowed_security_group_ids
    }
  }

  dynamic "ingress" {
    for_each = length(var.allowed_cidr_blocks) > 0 ? [1] : []
    content {
      description = "PostgreSQL from allowed CIDRs"
      from_port   = var.port
      to_port     = var.port
      protocol    = "tcp"
      cidr_blocks = var.allowed_cidr_blocks
    }
  }

  egress {
    description = "Allow all outbound traffic"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "demo-rds-sg-${var.environment}"
  }
}

# RDS Subnet Group spanning the private subnets
resource "aws_db_subnet_group" "rds" {
  name        = "demo-rds-subnet-group-${var.environment}"
  description = "Subnet group for RDS PostgreSQL across private subnets"
  subnet_ids  = var.subnet_ids

  tags = {
    Name = "demo-rds-subnet-group-${var.environment}"
  }
}

# PostgreSQL RDS Instance
resource "aws_db_instance" "postgres" {
  identifier             = "demo-postgres-${var.environment}"
  allocated_storage      = var.allocated_storage
  max_allocated_storage  = var.max_allocated_storage
  storage_type           = var.storage_type
  engine                 = var.engine
  engine_version         = var.engine_version
  instance_class         = var.db_instance_class
  db_name                = var.db_name
  username               = var.db_username
  password               = var.db_password
  db_subnet_group_name   = aws_db_subnet_group.rds.name
  vpc_security_group_ids = [aws_security_group.rds_sg.id]
  publicly_accessible    = var.publicly_accessible
  skip_final_snapshot    = var.skip_final_snapshot
  multi_az               = var.multi_az

  tags = {
    Name = "demo-postgres-db-${var.environment}"
  }
}
