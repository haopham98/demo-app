output "instance_ids" {
  description = "List of IDs of the EC2 instances"
  value       = aws_instance.web[*].id
}

output "public_ips" {
  description = "List of public IP addresses assigned to the EC2 instances"
  value       = aws_instance.web[*].public_ip
}

output "private_ips" {
  description = "List of private IP addresses assigned to the EC2 instances"
  value       = aws_instance.web[*].private_ip
}

output "security_group_id" {
  description = "The ID of the EC2 security group"
  value       = aws_security_group.ec2_sg.id
}
