#!/bin/bash

# --- Настройки ---
HELIOS="helios"
WILDFLY_CLI="~/Web/web4/bin/jboss-cli.sh"
DEPLOY_PATH="~/Web/web4/standalone/deployments/server.war"

FRONTEND_DIR="../frontend"

# --- Сборка фронта вручную ---
echo "=== Сборка фронтенда ==="
cd $FRONTEND_DIR
npm install
npm run build
cd -

# --- Сборка Maven WAR ---
echo "=== Сборка backend и WAR ==="
mvn clean install

# --- SCP загрузка на Helios ---
echo "=== Копирование WAR на Helios ==="
scp target/server.war $HELIOS:$DEPLOY_PATH

# --- Финал ---
echo "=== Деплой завершен ==="
echo "Теперь пробрось порт для локального доступа:"
echo "   ssh -L 8035:localhost:8035 -L 5432:localhost:5432 $HELIOS"
echo "Открой:"
echo "   http://localhost:8035/web4"