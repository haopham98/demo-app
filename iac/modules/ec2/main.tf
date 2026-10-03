# Data source: Latest Ubuntu 22.04 LTS (Jammy Jellyfish) AMI
data "aws_ami" "ubuntu" {
  most_recent = true

  filter {
    name   = "name"
    values = ["ubuntu/images/hvm-ssd/ubuntu-jammy-22.04-amd64-server-*"]
  }

  filter {
    name   = "virtualization-type"
    values = ["hvm"]
  }

  owners = ["099720109477"] # Canonical
}

# EC2 Security Group: Allow SSH port 22 and outbound traffic
resource "aws_security_group" "ec2_sg" {
  name        = "demo-ec2-sg-${var.environment}"
  description = "Security Group for EC2 instances in Public Subnet"
  vpc_id      = var.vpc_id

  ingress {
    description = "SSH access"
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = var.ssh_allowed_cidrs
  }

  egress {
    description = "Allow all outbound traffic"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "demo-ec2-sg-${var.environment}"
  }
}

# EC2 Instances
resource "aws_instance" "web" {
  count                       = var.instance_count
  ami                         = var.ami_id != "" ? var.ami_id : data.aws_ami.ubuntu.id
  instance_type               = var.instance_type
  subnet_id                   = var.subnet_id
  vpc_security_group_ids      = [aws_security_group.ec2_sg.id]
  associate_public_ip_address = true

  tags = {
    Name = "demo-ec2-${var.environment}-${count.index + 1}"
    Role = var.role_tag
  }
}
