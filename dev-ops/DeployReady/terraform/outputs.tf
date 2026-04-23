output "ec2_public_ip" {
  description = "The public IP address of the new EC2 instance"
  value       = aws_instance.app_server.public_ip
}

/*
output "pipeline_access_key" {
  description = "The AWS Access Key ID for the GitHub Actions pipeline"
  value       = aws_iam_access_key.github_actions_keys.id
}

output "pipeline_secret_key" {
  description = "The AWS Secret Access Key for the GitHub Actions pipeline"
  value       = aws_iam_access_key.github_actions_keys.secret
  sensitive   = true 
}
*/