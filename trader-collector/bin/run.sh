#!/bin/sh
set -eu

CONFIG_FILE=/app/config/config.properties
NODE_KEY=trader.node.id

mkdir -p /app/config
if [ ! -f "$CONFIG_FILE" ]; then
  touch "$CONFIG_FILE"
fi

EXISTING_LINE="$(grep -E "^${NODE_KEY}=" "$CONFIG_FILE" | tail -n 1 || true)"
if [ -n "$EXISTING_LINE" ]; then
  TRADER_NODE_ID="$(printf '%s' "$EXISTING_LINE" | cut -d '=' -f 2- | tr -d '\r\n')"
else
  TRADER_NODE_ID="$(cat /proc/sys/kernel/random/uuid | tr -d '\r\n')"
  if [ -s "$CONFIG_FILE" ]; then
    printf '\n%s=%s\n' "$NODE_KEY" "$TRADER_NODE_ID" >> "$CONFIG_FILE"
  else
    printf '%s=%s\n' "$NODE_KEY" "$TRADER_NODE_ID" >> "$CONFIG_FILE"
  fi
fi
export TRADER_NODE_ID
echo "TRADER_NODE_ID=[$TRADER_NODE_ID]"
exec java -jar trader-collector.jar --node.config.path=/app/config/config.properties --spring.config.location=file:/app/config/application.yml --logging.config=file:/app/config/logback-spring.xml
