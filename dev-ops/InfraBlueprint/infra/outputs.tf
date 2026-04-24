output "ec2_public_ip" {
  description = "Public IP address of the EC2 instance."
  value       = module.compute.ec2_public_ip
}

output "rds_endpoint" {
  description = "Connection endpoint for the RDS instance."
  value       = module.database.rds_endpoint
}

output "s3_bucket_name" {
  description = "Name of the S3 static assets bucket."
  value       = module.storage.bucket_name
}