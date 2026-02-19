#!/usr/bin/env bash
# ==============================================================
# dev.sh — Script de conveniência para desenvolvimento
# Uso: ./dev.sh [up|down|restart|logs|build]
# ==============================================================

set -e

COMPOSE_FILE="docker/compose.yml"

case "${1:-up}" in
  up)
    echo "🚀 Subindo todos os serviços..."
    docker compose -f "$COMPOSE_FILE" up --build -d
    echo ""
    echo "✅ Serviços rodando:"
    echo "   Backend  → http://localhost:8080"
    echo "   Frontend → http://localhost:3000"
    echo "   Swagger  → http://localhost:8080/swagger-ui.html"
    echo "   Postgres → localhost:5432"
    ;;
  down)
    echo "🛑 Parando todos os serviços..."
    docker compose -f "$COMPOSE_FILE" down
    ;;
  restart)
    echo "🔄 Reiniciando todos os serviços..."
    docker compose -f "$COMPOSE_FILE" down
    docker compose -f "$COMPOSE_FILE" up --build -d
    ;;
  logs)
    docker compose -f "$COMPOSE_FILE" logs -f
    ;;
  build)
    echo "🔨 Rebuild sem subir..."
    docker compose -f "$COMPOSE_FILE" build
    ;;
  *)
    echo "Uso: ./dev.sh [up|down|restart|logs|build]"
    exit 1
    ;;
esac
