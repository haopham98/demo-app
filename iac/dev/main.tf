# VPC Module
module "vpc" {
  source = "../modules/vpc"

  environment           = var.environment
  aws_region            = var.aws_region
  vpc_cidr              = var.vpc_cidr
  public_subnet_cidr    = var.public_subnet_cidr
  private_subnet_1_cidr = var.private_subnet_1_cidr
  private_subnet_2_cidr = var.private_subnet_2_cidr
}

# EC2 Module
module "ec2" {
  source = "../modules/ec2"

  environment    = var.environment
  vpc_id         = module.vpc.vpc_id
  subnet_id      = module.vpc.public_subnet_id
  instance_type  = var.instance_type
  instance_count = var.instance_count
}

# RDS Module
module "rds" {
  source = "../modules/rds"

  environment                = var.environment
  vpc_id                     = module.vpc.vpc_id
  subnet_ids                 = module.vpc.private_subnet_ids
  db_instance_class          = var.db_instance_class
  db_password                = var.db_password
  allowed_security_group_ids = [module.ec2.security_group_id]
}