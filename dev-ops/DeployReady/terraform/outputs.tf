output "instance_public_ip" {
  description = "Public IP of the EC2 instance"
  value       = aws_instance.deployready.public_ip
}

output "instance_id" {
  description = "EC2 instance ID"
  value       = aws_instance.deployready.id
}

output "ssh_command" {
  description = "SSH command to connect to the instance"
  value       = "ssh -i newkeypair.pem ubuntu@${aws_instance.deployready.public_ip}"
}
