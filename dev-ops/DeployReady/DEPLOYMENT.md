# Deployment Documentation

## 1. Cloud Provider and Service
**Cloud Provider:** AWS (Amazon Web Services)  
**Service:** Amazon EC2 (Elastic Compute Cloud)

**Why AWS EC2?**  
AWS EC2 was chosen because it provides full control over the virtual machine, which makes it straightforward to securely configure the firewall (Security Groups) and manually orchestrate Docker containers. The `t2.micro` instance type is highly cost-effective and falls within the AWS Free Tier, while offering sufficient compute for running a simple Node.js application container.

---

## 2. Infrastructure Setup (Bonus: Terraform)
To fulfill the infrastructure requirements and go a step further, **Terraform** was used to provision the resources. 

The configuration (`terraform/main.tf`) automatically handles:
- Provisioning an EC2 `t2.micro` instance running Ubuntu 22.04.
- Creating a Security Group that:
  - Opens Port 80 (HTTP) to the world (`0.0.0.0/0`).
  - Opens Port 22 (SSH) **only to your specific IP address** to maximize security.
- Injecting an SSH public key for secure access.

### How to set up the Virtual Machine:
1. Navigate to the `terraform/` directory.
2. Ensure you have the AWS CLI configured with your credentials.
3. Run `terraform init` to initialize the project.
4. Run `terraform apply` and confirm to create the resources.
5. Note the output variable `instance_public_ip` — this is your server's IP.

---

## 3. Installing Docker and Pulling the Image
Docker installation is fully automated via the **EC2 User Data script** defined in the Terraform file. Upon boot, the server automatically updates its packages, installs `docker-ce`, `docker-compose-plugin`, and adds the `ubuntu` user to the `docker` group.

### The Automated Deployment Pipeline
A GitHub Actions workflow (`.github/workflows/deploy.yml`) handles the process of pulling the new image and running it:
1. **Test**: The pipeline runs `npm test` to ensure code stability.
2. **Build and Push**: It builds the Docker image and pushes it to GitHub Container Registry (GHCR) tagged with the commit SHA.
3. **Deploy via SSH**: The pipeline connects to the EC2 instance using the `appleboy/ssh-action` plugin.
4. It logs into GHCR, pulls the newly built image (`docker pull`), stops any running containers, and runs the new image using `docker run` while binding port `80` on the host to port `3000` in the container.

### Setting up the Pipeline Secrets
To make the pipeline work, add the following secrets to your GitHub Repository (`Settings` > `Secrets and variables` > `Actions`):
- `SERVER_HOST`: The `instance_public_ip` outputted by Terraform.
- `SERVER_USERNAME`: `ubuntu`
- `SERVER_SSH_KEY`: The private key matching the public key you provided to Terraform.

---

## 4. Verification and Logs

### How to check if the container is running
SSH into your VM:
```bash
ssh -i ~/.ssh/id_rsa ubuntu@<your-server-ip>
```
Once inside, run the following command to see all active containers:
```bash
docker ps
```
You should see a container named `kora-app` running and mapping port `0.0.0.0:80->3000/tcp`.

You can also verify from your local machine using curl or a browser:
```bash
curl http://<your-server-ip>/health
```
*(Expected output: `{"status": "ok"}`)*

### How to view the application logs
While SSH'd into the server, use the Docker logs command:
```bash
docker logs kora-app
```
To tail the logs in real-time (useful for debugging live traffic), append the `-f` flag:
```bash
docker logs -f kora-app
```
