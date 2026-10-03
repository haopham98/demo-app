output "db_instance_id" {
  description = "The ID of the RDS instance"
  value       = aws_db_instance.postgres.id
}

output "db_endpoint" {
  description = "The connection endpoint for the RDS instance in address:port format"
  value       = aws_db_instance.postgres.endpoint
}

output "db_address" {
  description = "The hostname of the RDS instance"
  value       = aws_db_instance.postgres.address
}

output "db_port" {
  description = "The database port"
  value       = aws_db_instance.postgres.port
}

output "db_name" {
  description = "The database name"
  value       = aws_db_instance.postgres.db_name
}

output "security_group_id" {
  description = "The ID of the RDS security group"
  value       = aws_security_group.rds_sg.id
}

output "db_subnet_group_name" {
  description = "The name of the RDS DB subnet group"
  value       = aws_db_subnet_group.rds.name
}
