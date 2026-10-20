#!/bin/bash
set -e

echo "=========================================="
echo " Starting Student Management Setup on EC2 "
echo "=========================================="

# 1. Setup 2GB Swap Memory (Crucial for t2.micro / t3.micro with 1GB RAM)
if [ ! -f /swapfile ]; then
    echo "Configuring 2GB swap file to prevent OOM errors..."
    sudo fallocate -l 2G /swapfile || sudo dd if=/dev/zero of=/swapfile bs=1M count=2048
    sudo chmod 600 /swapfile
    sudo mkswap /swapfile
    sudo swapon /swapfile
    echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
    echo "Swap created successfully."
else
    echo "Swap file already exists."
fi

# 2. Update and install prerequisites
echo "Updating packages..."
sudo apt-get update -y
sudo apt-get install -y ca-certificates curl gnupg lsb-release git

# 3. Install Docker if not present
if ! command -v docker &> /dev/null; then
    echo "Installing Docker..."
    curl -fsSL https://get.docker.com -o get-docker.sh
    sudo sh get-docker.sh
    sudo usermod -aG docker "$USER"
    sudo systemctl enable docker
    sudo systemctl start docker
    rm get-docker.sh
    echo "Docker installed successfully."
fi

# 4. Install Docker Compose if not present
if ! command -v docker-compose &> /dev/null; then
    echo "Installing Docker Compose..."
    sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
    sudo chmod +x /usr/local/bin/docker-compose
    echo "Docker Compose installed successfully."
fi

# 5. Build and launch services
echo "Building and starting Docker containers..."
sudo docker-compose up -d --build

echo "=========================================="
echo " Deployment Complete! "
echo " Application running on port 80 (HTTP)    "
echo " Backend API running on port 8080         "
echo "=========================================="
