# Kora App — Production Deployment Guide

This document describes how the Kora Analytics API was containerized, deployed, and automated using CI/CD pipelines and AWS infrastructure.

## 1. Cloud Provider & Architecture Choice

### Cloud Provider
**Amazon Web Services (AWS EC2)**

#### Why AWS?
AWS was selected because:
- Industry-standard cloud platform
- Free-tier eligible EC2 instances (t3.micro)
- Strong support for Docker-based deployments
- Simple SSH-based server management
- Widely used in real production environments

### Architecture Overview
```text
Developer Push
      ↓
GitHub Actions CI/CD
      ↓
Run Tests (npm test)
      ↓
Build Docker Image (commit SHA tag)
      ↓
Push to GitHub Container Registry (GHCR)
      ↓
SSH Deploy to EC2
      ↓
Docker Container Running
      ↓
Public API (HTTP 80)
```

## 2. Virtual Machine Setup (EC2)

### Instance Configuration
- **AMI:** Ubuntu Server 26.04 LTS
- **Instance Type:** t3.micro (Free Tier)
- **Region:** us-east-1
- **Storage:** 8 GB gp3

### Security Group Configuration
| Type | Port | Source |
|------|------|--------|
| SSH  | 22   | My IP only |
| HTTP | 80   | 0.0.0.0/0 |

✔ SSH access is restricted for security  
✔ Only HTTP is publicly exposed

### EC2 Access
```bash
ssh -i server2pem.pem ubuntu@ec2-54-227-156-238.compute-1.amazonaws.com
```

## 3. Docker Installation & Setup

### Install Docker
```bash
sudo apt update
sudo apt install -y docker.io
sudo systemctl enable docker
sudo systemctl start docker
sudo usermod -aG docker ubuntu
```

Verify installation:
```bash
docker --version
```

## 4. Application Deployment

### Pull Latest Image
```bash
docker pull ghcr.io/muhireyves250/amalitech-deg-project-based-challenges/kora-app:71e5064e7b1f7738a512f4f6d07bb5533add13a5
```

### Run Container
```bash
docker run -d \
  --name kora-app \
  -p 80:3000 \
  ghcr.io/muhireyves250/amalitech-deg-project-based-challenges/kora-app:71e5064e7b1f7738a512f4f6d07bb5533add13a5
```

### Verify Running Container
```bash
docker ps
```

Expected:
- Container running: `kora-app`
- Port mapping: 80 → 3000

## 5. CI/CD Pipeline (GitHub Actions)

### Pipeline File
`.github/workflows/deploy.yml`

### Pipeline Stages

#### 1. Test Stage
- Runs `npm test`
- Stops pipeline if tests fail

#### 2. Build Stage
- Builds Docker image
- Tags image with Git commit SHA

#### 3. Push Stage
- Pushes image to GitHub Container Registry (GHCR)

#### 4. Deploy Stage
- SSH into EC2
- Pull latest image
- Restart running container

### GitHub Secrets Required
| Secret Name | Purpose |
|-------------|---------|
| `EC2_SSH_KEY` | Private SSH key for server access |
| `EC2_SERVER_IP` | EC2 public IP |
| `GITHUB_TOKEN` | GHCR authentication |

## 6. Health Check (Deployment Verification)

### Endpoint
`GET /health`

### Test Command
```bash
curl http://ec2-54-227-156-238.compute-1.amazonaws.com/health
```

### Expected Response
```json
{
  "status": "ok"
}
```
✔ Confirms successful deployment

## 7. Monitoring & Logs

### Check Running Container
```bash
docker ps
```

### View Logs
```bash
docker logs -f kora-app
```

### Monitor Resource Usage
```bash
docker stats kora-app
```

## 8. Security Implementation

This deployment follows DevOps best practices:
- Container runs as a non-root user
- SSH key stored securely in GitHub Secrets
- No `.pem` or `.env` files committed to repository
- EC2 SSH restricted to specific IP only
- Only HTTP (port 80) exposed publicly
- Images pulled securely from GHCR

## 9. Deployment Workflow

Every push to main triggers:
- ✔ Run tests
- ✔ Build Docker image
- ✔ Push to GHCR
- ✔ Deploy to EC2

## 10. Key Learnings

- CI/CD automation using GitHub Actions
- Docker container lifecycle management
- AWS EC2 deployment and networking
- Secure secret handling in pipelines
- Production-grade deployment workflow design

## 11. Live Deployment

- **Base URL**: `http://ec2-54-227-156-238.compute-1.amazonaws.com`
- **Health Check**: `http://ec2-54-227-156-238.compute-1.amazonaws.com/health`

## 12. Troubleshooting Notes

- **Issue**: Docker build failed
  - **Cause**: incorrect path in workflow
  - **Fix**: corrected build context in GitHub Actions
- **Issue**: SSH authentication failed
  - **Cause**: missing or incorrect private key in secrets
  - **Fix**: added correct `EC2_SSH_KEY` in GitHub Secrets

## Summary

This project demonstrates a complete DevOps pipeline:  
**Code → Test → Build → Push → Deploy → Monitor**

- ✔ Fully automated CI/CD
- ✔ Cloud deployment on AWS EC2
- ✔ Docker-based containerization
- ✔ Secure production configuration
