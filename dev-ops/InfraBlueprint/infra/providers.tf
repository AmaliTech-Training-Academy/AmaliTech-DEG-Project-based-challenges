terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 6.0"
    }
  }

  # Uncomment and configure once your S3 backend bucket exists.
  # backend "s3" {
  #   bucket = "your-terraform-state-bucket"
  #   key    = "vela-payments/terraform.tfstate"
  #   region = "us-east-1"
  #   encrypt = true
  # }
}

provider "aws" {
  region = var.aws_region
  default_tags {
    tags = {
      Project = "vela-payments"
    }
  }
}