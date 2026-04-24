# terraform {
#   required_version = ">= 1.5"

#   required_providers {
#     aws = {
#       source  = "hashicorp/aws"
#       version = "~> 6.0"
#     }
#   }

# Uncomment and configure once your S3 backend bucket exists.
# backend "s3" {
#   bucket = "your-terraform-state-bucket"
#   key    = "vela-payments/terraform.tfstate"
#   region = var.aws_region
# }
# }

# provider "aws" {
#   region = var.aws_region
# }
