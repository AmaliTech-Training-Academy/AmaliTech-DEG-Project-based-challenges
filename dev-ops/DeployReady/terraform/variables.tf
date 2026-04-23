variable "aws_region" {
  description = "The AWS region to deploy to"
  type        = string
  default     = "eu-west-1" 
}

variable "aws_profile" {
  description = "The AWS CLI profile to use"
  type        = string
  default     = "emmiduh-amalitech"
}

variable "key_name" {
  description = "The name of the existing AWS key pair"
  type        = string
  default     = "amalitech-devops-key"
}

variable "my_ip" {
  description = "My personal public IP address"
  type        = string
  default     = "196.12.152.98/32"
}