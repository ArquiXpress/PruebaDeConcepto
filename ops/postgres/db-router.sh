#!/bin/sh
set -eu

STATE_FILE="/state/active_host"
CURRENT_TARGET=""
SOCAT_PID=""

write_default_target() {
  if [ ! -s "$STATE_FILE" ]; then
    echo "postgres" > "$STATE_FILE"
  fi
}

start_proxy() {
  TARGET="$(cat "$STATE_FILE")"
  if [ "$TARGET" = "$CURRENT_TARGET" ] && [ -n "$SOCAT_PID" ] && kill -0 "$SOCAT_PID" >/dev/null 2>&1; then
    return
  fi
  if [ -n "$SOCAT_PID" ] && kill -0 "$SOCAT_PID" >/dev/null 2>&1; then
    kill "$SOCAT_PID" >/dev/null 2>&1 || true
    wait "$SOCAT_PID" >/dev/null 2>&1 || true
  fi
  CURRENT_TARGET="$TARGET"
  socat TCP-LISTEN:5432,fork,reuseaddr TCP:"$TARGET":5432 &
  SOCAT_PID=$!
}

apk add --no-cache socat >/dev/null
write_default_target

while true; do
  start_proxy
  sleep 1
done
