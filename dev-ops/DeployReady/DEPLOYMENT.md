# Deployment Documentation

## Cloud Provider

**Provider:** Amazon Web Services (AWS)  
**Service:** EC2 (Elastic Compute Cloud)  
**Instance Type:** t3.micro (1 vCPU, 1 GiB RAM)

### Why AWS EC2?

- **Free Tier Eligible:** t3.micro instances are included in the AWS free tier, making it cost-effective for this project
- **Industry Standard:** AWS is the most widely used cloud provider, making this experience valuable
- **Simple Setup:** EC2 provides straightforward VM provisioning with good documentation
- **Docker Compatible:** Ubuntu on EC2 works seamlessly with Docker containers

---

## Virtual Machine Setup

### 1. Instance Configuration

- **AMI:** Ubuntu Server 26.04 LTS
- **Instance Type:** t3.micro
- **Region:** us-east-1 (N. Virginia)
- **Storage:** 8 GiB gp3 root volume

### 2. Security Group Configuration

The security group controls network access to the instance:

| Type | Protocol | Port | Source | Purpose |
|------|----------|------|--------|---------|
| SSH | TCP | 22 | 0.0.0.0/0 | Remote server management (GitHub Actions deployment) |
| HTTP | TCP | 80 | 0.0.0.0/0 | Public web traffic |

**Note:** SSH is currently open to all IPs (`0.0.0.0/0`) to allow GitHub Actions runners to connect. In production, this should be restricted using AWS Systems Manager Session Manager or a bastion host.

### 3. Key Pair

- **Name:** newkeypair
- **Type:** RSA
- **Format:** .pem
- **Storage:** Stored securely as a GitHub Secret (`EC2_SSH_KEY`)

---

## Docker Installation

Docker was installed on the Ubuntu instance using the following commands:

```bash
# Update package list
sudo apt update

# Install Docker
sudo apt install docker.io -y

# Start Docker service and enable on boot
sudo systemctl start docker
sudo systemctl enable docker

# Add ubuntu user to docker group (allows running docker without sudo)
sudo usermod -aG docker ubuntu
```

### Verify Installation

```bash
docker --version
# Output: Docker version 29.1.3, build 29.1.3-0ubuntu4
```

---

## Container Registry

**Registry:** GitHub Container Registry (GHCR)  
**Image:** `ghcr.io/trtheo/deployready:latest`

The Docker image is automatically built and pushed by the GitHub Actions pipeline on every push to the `main` branch.

---

## Deployment Process

### Automated Deployment (CI/CD)

The deployment is fully automated via GitHub Actions (`.github/workflows/deploy.yml`):

1. **Test:** Runs `npm test` in the app directory
2. **Build:** Builds Docker image tagged with commit SHA and `latest`
3. **Push:** Pushes image to GitHub Container Registry
4. **Deploy:** SSHs into EC2, pulls the new image, and restarts the container

### Manual Deployment

If you need to deploy manually:

```bash
# SSH into the server
ssh -i newkeypair.pem ubuntu@54.89.125.94

# Pull the latest image
docker pull ghcr.io/trtheo/deployready:latest

# Stop and remove old container
docker stop deployready
docker rm deployready

# Run new container
docker run -d --name deployready -p 80:3000 -e PORT=3000 --restart unless-stopped ghcr.io/trtheo/deployready:latest
```

---

## Checking Container Status

### Is the container running?

```bash
docker ps
```

**Expected output:**
```
CONTAINER ID   IMAGE                                  COMMAND           CREATED          STATUS          PORTS                                   NAMES
abc123def456   ghcr.io/trtheo/deployready:latest     "node index.js"   10 minutes ago   Up 10 minutes   0.0.0.0:80->3000/tcp, :::80->3000/tcp   deployready
```

### Container details

```bash
docker inspect deployready
```

---

## Viewing Application Logs

### Real-time logs (follow mode)

```bash
docker logs -f deployready
```

Press `Ctrl+C` to stop following.

### Last 50 lines

```bash
docker logs --tail 50 deployready
```

### Logs with timestamps

```bash
docker logs -t deployready
```

---

## Health Check

The application exposes a health endpoint:

```bash
curl http://54.89.125.94/health
```

**Expected response:**
```json
{
  "status": "ok"
}
```

---

## Troubleshooting

### Container won't start

```bash
# Check container logs for errors
docker logs deployready

# Check if port 80 is already in use
sudo netstat -tulpn | grep :80
```

### Can't connect to the application

```bash
# Verify container is running
docker ps

# Check security group allows HTTP on port 80
# Check instance public IP hasn't changed
```

### SSH connection issues

```bash
# Verify security group allows SSH from your IP
# Verify .pem file permissions (should be 600)
chmod 600 newkeypair.pem
```

---

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                         GitHub                              │
│  ┌──────────────────────────────────────────────────────┐   │
│  │  Push to main branch                                 │   │
│  │         ↓                                            │   │
│  │  GitHub Actions Workflow                             │   │
│  │    1. Run tests                                      │   │
│  │    2. Build Docker image                             │   │
│  │    3. Push to GHCR                                   │   │
│  │    4. SSH to EC2 → Deploy                            │   │
│  └──────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
                           │
                           │ SSH (port 22)
                           ↓
┌─────────────────────────────────────────────────────────────┐
│                    AWS EC2 Instance                         │
│                  (Ubuntu 26.04 LTS)                         │
│                                                             │
│  ┌───────────────────────────────────────────────────────┐  │
│  │  Docker Container: deployready                        │  │
│  │  Image: ghcr.io/trtheo/deployready:latest            │  │
│  │  Port mapping: 80 → 3000                              │  │
│  │                                                       │  │
│  │  ┌─────────────────────────────────────────────────┐ │  │
│  │  │  Node.js Application                            │ │  │
│  │  │  - GET  /health                                 │ │  │
│  │  │  - GET  /metrics                                │ │  │
│  │  │  - POST /data                                   │ │  │
│  │  └─────────────────────────────────────────────────┘ │  │
│  └───────────────────────────────────────────────────────┘  │
│                                                             │
│  Security Group:                                            │
│    - SSH (22) from 0.0.0.0/0                                │
│    - HTTP (80) from 0.0.0.0/0                               │
└─────────────────────────────────────────────────────────────┘
                           │
                           │ HTTP (port 80)
                           ↓
                    ┌─────────────┐
                    │   Internet  │
                    │   Users     │
                    └─────────────┘
```

---

## Public Endpoint

**URL:** http://54.89.125.94

**Available Routes:**
- `GET /health` : Returns service health status
- `GET /metrics` : Returns uptime and memory usage
- `POST /data` : Echoes back JSON payload

---

## Security Considerations

### Current Setup
- SSH port 22 is open to the internet (`0.0.0.0/0`) to allow GitHub Actions deployment
- No HTTPS/SSL configured (HTTP only)
- Container runs as non-root user for security

### Production Recommendations
- Use AWS Systems Manager Session Manager instead of SSH
- Implement HTTPS with Let's Encrypt SSL certificate
- Use AWS Secrets Manager for sensitive credentials
- Restrict SSH to specific IP ranges or use a bastion host
- Enable AWS CloudWatch monitoring and alarms
- Implement rate limiting and DDoS protection with AWS WAF

---

## Cost Estimate

**Current monthly cost:** $0 (within AWS free tier)

**After free tier expires:**
- t3.micro instance: ~$7.50/month
- 8 GiB EBS storage: ~$0.80/month
- Data transfer: ~$0.09/GB (first 100 GB free)

**Total estimated cost:** ~$8-10/month

---

## Bonus: Terraform Infrastructure as Code

### What was added

A `terraform/` directory with 3 files to provision the EC2 instance and security group:

- `main.tf` — defines the EC2 instance and security group (SSH port 22, HTTP port 80)
- `variables.tf` — configurable inputs (region, AMI, instance type, key pair)
- `outputs.tf` — prints the public IP, instance ID, and SSH command after provisioning

### Why

- **Reproducible:** Infrastructure can be recreated with a single command instead of clicking through the AWS console
- **Version controlled:** Infrastructure changes are tracked in Git alongside application code
- **Consistent:** Eliminates manual configuration errors

### How to use

```bash
cd terraform
terraform init
terraform plan
terraform apply
```

To destroy the infrastructure:
```bash
terraform destroy
```

---

## Bonus: Rollback on Failed Health Check

### What was added

The deploy step in `.github/workflows/deploy.yml` now includes an automatic rollback mechanism:

1. **Saves the previous image** tag before deploying the new one
2. **Deploys the new image** tagged with the commit SHA
3. **Runs a health check** against `/health` — retries up to 5 times (5s apart)
4. **Rolls back** to the previous image if all health checks fail, then exits with error to mark the pipeline as failed

### Why

- Prevents a broken deployment from taking down the live application
- Ensures users always have a working version running
- Uses commit SHA tags (not just `latest`) so the previous image is always retrievable

---

## Maintenance

### Updating the application

Simply push changes to the `main` branch , the CI/CD pipeline handles everything automatically.

### Updating Docker

```bash
sudo apt update
sudo apt upgrade docker.io -y
sudo systemctl restart docker
```

### Monitoring disk space

```bash
df -h
docker system df
```

### Cleaning up old images

```bash
docker system prune -a
```
