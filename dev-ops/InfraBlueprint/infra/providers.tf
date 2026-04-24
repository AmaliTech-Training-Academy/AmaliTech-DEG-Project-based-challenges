terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 6.0"
    }
  }

  # Uncomment and configure once your S3 backend bucket exists.
  backend "s3" {
    bucket  = "vela-assets-2026"
    key     = "vela-assets/terraform.tfstate"
    region  = "eu-west-1"
    encrypt = true
  }
}

provider "aws" {
  region = var.aws_region
  default_tags {
    tags = {
      Project = "vela-payments"
    }
  }
}