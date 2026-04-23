terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 6.0"
    }
  }
}

# Configure the AWS Provider
provider "aws" {
  region  = var.aws_region
  profile = var.aws_profile
}

# Automatically fetch the latest Amazon Linux 2023 OS image
data "aws_ami" "amazon_linux_2023" {
  most_recent = true
  owners      = ["amazon"]

  filter {
    name   = "name"
    values = ["al2023-ami-2023.*-x86_64"]
  }
}

# Create a Security Group
resource "aws_security_group" "api_sg" {
  name        = "deployready_api_sg"
  description = "Allow SSH and HTTP inbound traffic"

  ingress {
    description = "SSH from my IP only"
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = [var.my_ip]
  }

  ingress {
    description = "HTTP for API"
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    description = "Allow all outbound traffic"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

# Provision the actual EC2 Server
resource "aws_instance" "app_server" {
  ami           = data.aws_ami.amazon_linux_2023.id
  instance_type = "t3.micro" # t2.micro instance type is not eligible for Free Tier
  key_name      = var.key_name

  vpc_security_group_ids = [aws_security_group.api_sg.id]

  # Install Docker automatically when the server boots up
  user_data = <<-EOF
              #!/bin/bash
              dnf update -y
              dnf install -y docker
              systemctl enable docker
              systemctl start docker
              usermod -aG docker ec2-user
              EOF

  tags = {
    Name = "DeployReady-API-Server"
  }
}

/*
# Create an IAM User for the CI/CD Pipeline
resource "aws_iam_user" "github_actions_user" {
  name = "deployready-pipeline-user"
  path = "/system/"
}

# Generate Access Keys for the Pipeline User
resource "aws_iam_access_key" "github_actions_keys" {
  user = aws_iam_user.github_actions_user.name
}

# Attach a strict Least-Privilege Policy to the User
resource "aws_iam_user_policy" "pipeline_policy" {
  name = "DeployReady-EC2-Minimal-Access"
  user = aws_iam_user.github_actions_user.name

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect   = "Allow"
        Action   = "ec2:DescribeInstances"
        Resource = "*"
      },
      {
        Effect   = "Allow"
        Action   = [
          "ec2:StartInstances",
          "ec2:StopInstances"
        ]
        # This explicitly restricts the pipeline to ONLY touch this exact server
        Resource = aws_instance.app_server.arn 
      }
    ]
  })
}
*/