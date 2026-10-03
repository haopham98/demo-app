output "vpc_id" {
  description = "The ID of the VPC"
  value       = aws_vpc.main.id
}

output "vpc_cidr" {
  description = "The CIDR block of the VPC"
  value       = aws_vpc.main.cidr_block
}

output "internet_gateway_id" {
  description = "The ID of the Internet Gateway"
  value       = aws_internet_gateway.gw.id
}

output "public_subnet_id" {
  description = "The ID of the Public Subnet"
  value       = aws_subnet.public.id
}

output "private_subnet_1_id" {
  description = "The ID of Private Subnet 1"
  value       = aws_subnet.private_1.id
}

output "private_subnet_2_id" {
  description = "The ID of Private Subnet 2"
  value       = aws_subnet.private_2.id
}

output "private_subnet_ids" {
  description = "List of IDs of the Private Subnets"
  value       = [aws_subnet.private_1.id, aws_subnet.private_2.id]
}

output "public_route_table_id" {
  description = "The ID of the Public Route Table"
  value       = aws_route_table.public.id
}
