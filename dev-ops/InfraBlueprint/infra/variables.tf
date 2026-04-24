variable "aws_region" {
  description = "AWS region to deploy all resources into."
  type        = string
}

variable "vpc_cidr" {
  description = "CIDR block for the VPC."
  type        = string
  default     = "10.0.0.0/16"
}

variable "allowed_ssh_cidr" {
  description = "Your IP address in CIDR notation allowed to SSH into the EC2 instance (e.g. 203.0.113.5/32)."
  type        = string
}

variable "db_username" {
  description = "Master username for the RDS instance."
  type        = string
  sensitive   = true
}

variable "db_password" {
  description = "Master password for the RDS instance."
  type        = string
  sensitive   = true
}

variable "s3_bucket_name" {
  description = "Globally unique name for the S3 static assets bucket."
  type        = string
}