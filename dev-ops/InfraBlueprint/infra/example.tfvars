# Example variable values for `terraform plan -var-file=example.tfvars`.
# Replace placeholders with your own values. Do NOT commit real credentials or your real IP to VCS.

aws_region = "us-east-1"

vpc_cidr = "10.0.0.0/16"

allowed_ssh_cidr = "203.0.113.5/32" # FAKE IP: Replace with your actual IP address/32"

# RDS master credentials — MUST be supplied; no defaults exist in Terraform.
db_username = "vela_admin"
db_password = "REPLACE_WITH_STRONG_PASSWORD_MEETING_RDS_POLICY"

# Globally unique S3 bucket name (lower case, no underscores if you want strict DNS-style names).
bucket_name = "vela-payments-app-data-UNIQUE_SUFFIX"

# existing EC2 key pair name in the same region for SSH. Set to null if unused.
ec2_key_pair_name = null
