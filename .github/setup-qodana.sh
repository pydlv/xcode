#!/bin/bash
# Setup script for Qodana CLI in Copilot environment

set -e

echo "Installing Qodana CLI..."

# Create directory for Qodana CLI
sudo mkdir -p /usr/local/bin

# Download and install Qodana CLI
QODANA_VERSION="v2025.1.2"
QODANA_URL="https://github.com/JetBrains/qodana-cli/releases/download/${QODANA_VERSION}/qodana_linux_x86_64"

echo "Downloading Qodana CLI ${QODANA_VERSION}..."
sudo curl -L "${QODANA_URL}" -o /usr/local/bin/qodana

# Make it executable
sudo chmod +x /usr/local/bin/qodana

# Verify installation
echo "Verifying Qodana installation..."
qodana --version

echo "Qodana CLI installation complete!"
echo "Available commands:"
echo "  qodana scan - Run code quality analysis"
echo "  qodana --help - Show all available commands"