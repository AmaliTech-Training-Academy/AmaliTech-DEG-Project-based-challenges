# DeployReady: Kora Analytics Infrastructure

This repository contains the infrastructure, containerization, and continuous delivery configuration for the Kora Analytics API.

## Architecture Overview

The application has been modernized from a manually-deployed process into a fully automated, containerized pipeline.

1. **Application Containerization:** The Node.js application is packaged using Docker. The `Dockerfile` implements security best practices by running as a non-root user (`node`) and using a lightweight Alpine base image (`node:20-alpine`).
2. **Local Orchestration:** Developers can spin up the environment locally using `docker compose`, which loads configuration from a `.env` file.
3. **CI/CD Pipeline:** A GitHub Actions workflow (`deploy.yml`) handles testing, building, and deployment automatically upon every push to the `main` branch. It utilizes GitHub Container Registry (GHCR) as the image repository.
4. **Cloud Infrastructure:** The application is deployed to an AWS EC2 instance. The infrastructure is defined using **Terraform** (`terraform/main.tf`), allowing for reproducible, automated provisioning of the virtual machine and strict security group rules.

## Decisions Made

- **Terraform for Infrastructure as Code (IaC):** To eliminate manual clicking through the AWS console and guarantee reproducible environments, Terraform was selected to provision the EC2 instance and Security Groups.
- **GitHub Container Registry (GHCR):** GHCR integrates natively with GitHub Actions and repository permissions, eliminating the need to manage third-party registry tokens.
- **SSH-based Deployment:** A lightweight, pull-based deployment strategy via SSH was chosen. The pipeline connects to the server, pulls the new image, and restarts the container. This is simple, effective, and avoids the overhead of managing a heavy orchestration tool like Kubernetes for a single container.
- **Strict Security Groups:** The firewall rules automatically dynamically restrict SSH access (Port 22) to only the executor's IP address, mitigating unauthorized access attempts.

## Setup Steps

### 1. Local Development
To run the API locally on your machine:
```bash
# Clone the repository
git clone <repository-url>
cd DeployReady

# Copy the environment template
cp .env.example .env

# Build and run the container
docker compose up --build
```
The API will be accessible at `http://localhost:3000`.

### 2. Infrastructure Provisioning (AWS)
Ensure you have the AWS CLI installed and configured.
```bash
cd terraform
terraform init
terraform apply
```
When prompted, type `yes` to provision the resources. Note the `instance_public_ip` output at the end.

### 3. CI/CD Pipeline Setup
To enable the automated deployment pipeline, add the following Repository Secrets to your GitHub repository:
- `SERVER_HOST`: The IP address of your EC2 instance (from the Terraform output).
- `SERVER_USERNAME`: `ubuntu`
- `SERVER_SSH_KEY`: The private SSH key corresponding to the public key provided to Terraform.

Once configured, pushing any code change to the `main` branch will automatically test, build, and deploy the new version to your cloud server!

For more details on interacting with the running server, see [DEPLOYMENT.md](./DEPLOYMENT.md).
