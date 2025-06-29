#!/bin/bash

# Estimoo Backend Development Script
set -e

echo "🚀 Starting Estimoo Backend Development Environment..."

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo "❌ Docker is not running. Please start Docker first."
    exit 1
fi

# Stop existing containers if running
echo "🛑 Stopping existing containers..."
docker-compose down || true

# Start PostgreSQL and Backend with development profile
echo "▶️ Starting development environment..."
docker-compose up -d

# Wait for PostgreSQL to be ready
echo "⏳ Waiting for PostgreSQL to be ready..."
sleep 10

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
    docker-compose logs estimoo-backend
    exit 1
fi

echo "🎉 Development environment is ready!"
echo "🌐 API is available at: http://localhost:8080"
echo "🗄️ PostgreSQL is available at: localhost:5432"
echo "🔍 Health check: http://localhost:8080/api/health"
echo ""
echo "📋 Useful commands:"
echo "  docker-compose logs -f          # View logs"
echo "  docker-compose down             # Stop all services"
echo "  docker-compose restart          # Restart services" 