#!/bin/bash

# Estimoo Backend Deployment Script
set -e

# Check if environment file exists
if [ ! -f ".env.prod" ]; then
    echo "❌ .env.prod file not found!"
    echo "📝 Please create .env.prod file based on env.prod.example"
    echo "💡 Example: cp env.prod.example .env.prod"
    exit 1
fi

# Load environment variables
source .env.prod

echo "🚀 Starting Estimoo Backend Deployment..."

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo "❌ Docker is not running. Please start Docker first."
    exit 1
fi

# Build the application
echo "📦 Building the application..."
./mvnw clean package -DskipTests

# Build Docker image
echo "🐳 Building Docker image..."
docker build -t estimoo-backend:latest .

# Stop existing container if running
echo "🛑 Stopping existing container..."
docker stop estimoo-backend || true
docker rm estimoo-backend || true

# Start new container with production environment
echo "▶️ Starting new container..."
docker run -d \
    --name estimoo-backend \
    --restart unless-stopped \
    -p 8080:8080 \
    -e SPRING_PROFILES_ACTIVE=prod \
    -e DB_HOST="$DB_HOST" \
    -e DB_PORT="$DB_PORT" \
    -e DB_NAME="$DB_NAME" \
    -e DB_USERNAME="$DB_USERNAME" \
    -e DB_PASSWORD="$DB_PASSWORD" \
    --memory=1.5g \
    --cpus=1.5 \
    estimoo-backend:latest

# Wait for application to start
echo "⏳ Waiting for application to start..."
sleep 30

# Health check
echo "🏥 Performing health check..."
if curl -f http://localhost:8080/api/health > /dev/null 2>&1; then
    echo "✅ Application is healthy!"
    echo "📊 Health check response:"
    curl -s http://localhost:8080/api/health | jq . || curl -s http://localhost:8080/api/health
else
    echo "❌ Health check failed!"
    echo "📋 Container logs:"
    docker logs estimoo-backend
    exit 1
fi

echo "🎉 Deployment completed successfully!"
echo "🌐 API is available at: http://localhost:8080"
echo "🔍 Health check: http://localhost:8080/api/health"
echo "📈 Metrics: http://localhost:8080/api/health/metrics" 