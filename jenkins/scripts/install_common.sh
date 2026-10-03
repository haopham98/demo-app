#!/bin/bash
set -euo pipefail

# 1. Cập nhật hệ thống
sudo apt update && sudo apt upgrade -y

# 2. Cài đặt các công cụ cơ bản & Java
sudo apt install -y git unzip curl gnupg software-properties-common lsb-release
sudo apt install -y openjdk-21-jdk openjdk-21-jre

# 3. Thêm Jenkins Repository Key (đã chuyển đổi sang gpg nhị phân)
curl -fsSL https://pkg.jenkins.io/debian-stable/jenkins.io-2023.key | sudo gpg --dearmor -o /usr/share/keyrings/jenkins-keyring.gpg --yes

# Thêm Jenkins APT repository
sudo wget -O /etc/apt/keyrings/jenkins-keyring.asc https://pkg.jenkins.io/debian-stable/jenkins.io-2026.key
echo "deb [signed-by=/etc/apt/keyrings/jenkins-keyring.asc]" https://pkg.jenkins.io/debian-stable binary/ | sudo tee /etc/apt/sources.list.d/jenkins.list > /dev/null
sudo apt update
sudo apt install jenkins



# 4. Cài đặt Jenkins
sudo apt update
sudo apt install -y jenkins

# Bật và khởi chạy Jenkins service
sudo systemctl enable --now jenkins

# 5. Thêm HashiCorp (Terraform) Repository Key
wget -O- https://apt.releases.hashicorp.com/gpg | sudo gpg --dearmor -o /usr/share/keyrings/hashicorp-archive-keyring.gpg --yes

# Thêm HashiCorp repository (Đã sửa lại đường dẫn /etc/apt/...)
echo "deb [signed-by=/usr/share/keyrings/hashicorp-archive-keyring.gpg] https://apt.releases.hashicorp.com $(lsb_release -cs) main" | sudo tee /etc/apt/sources.list.d/hashicorp.list > /dev/null

# Cài đặt Terraform
sudo apt update && sudo apt install -y terraform

# 6. Cài đặt AWS CLI v2
cd /tmp
curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
unzip -q awscliv2.zip
sudo ./aws/install --update
rm -rf aws awscliv2.zip

echo "=== Successfull Installation ==="