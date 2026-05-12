#!/usr/bin/env bash
# HPC tunnel management — SSH port-forward to a CREATE compute node via login node
# Usage:
#   ./scripts/hpc-tunnel.sh start   — open API tunnel
#   ./scripts/hpc-tunnel.sh stop    — close tunnel
#   ./scripts/hpc-tunnel.sh status  — check if tunnel is running
#
# Update REMOTE_HOST if the SongGeneration server moves to a different node.
# The working directory on the remote (/users/k1810895/data/musicgen) is stable.

PIDFILE="$HOME/.hpc-tunnel.pid"
LOCAL_PORT=8888               # localhost:8888 → <REMOTE_HOST>:<REMOTE_PORT> (SongGeneration API)
REMOTE_HOST="erc-hpc-comp232" # ← update this if the node changes
REMOTE_PORT=8888
LOGIN_NODE="k1810895@hpc.create.kcl.ac.uk"

start() {
  if [ -f "$PIDFILE" ] && kill -0 "$(cat "$PIDFILE")" 2>/dev/null; then
    echo "Tunnel already running (PID $(cat "$PIDFILE"))"
    return
  fi

  echo "Opening tunnel to $REMOTE_HOST:$REMOTE_PORT via $LOGIN_NODE..."
  ssh -fNL "$LOCAL_PORT:$REMOTE_HOST:$REMOTE_PORT" "$LOGIN_NODE"

  PID=$(pgrep -f "ssh -fNL $LOCAL_PORT:$REMOTE_HOST" | head -1)
  if [ -n "$PID" ]; then
    echo "$PID" > "$PIDFILE"
    echo "Tunnel open (PID $PID)"
    echo "  SongGen API: localhost:$LOCAL_PORT → $REMOTE_HOST:$REMOTE_PORT"
  else
    echo "Failed to start tunnel — check MFA at https://portal.er.kcl.ac.uk/mfa/"
    exit 1
  fi
}

stop() {
  if [ ! -f "$PIDFILE" ]; then
    echo "No tunnel PID file found"
    return
  fi

  PID=$(cat "$PIDFILE")
  if kill -0 "$PID" 2>/dev/null; then
    kill "$PID"
    rm -f "$PIDFILE"
    echo "Tunnel closed (PID $PID)"
  else
    echo "Tunnel was not running (stale PID file removed)"
    rm -f "$PIDFILE"
  fi
}

status() {
  if [ -f "$PIDFILE" ] && kill -0 "$(cat "$PIDFILE")" 2>/dev/null; then
    echo "Tunnel is running (PID $(cat "$PIDFILE"))"
    echo "  SongGen API: localhost:$LOCAL_PORT → $REMOTE_HOST:$REMOTE_PORT"
    curl -s --max-time 5 "http://localhost:$LOCAL_PORT/health" && echo "" || echo "  API health: unreachable"
  else
    echo "Tunnel is not running"
  fi
}

case "${1:-}" in
  start)  start  ;;
  stop)   stop   ;;
  status) status ;;
  *)
    echo "Usage: $0 {start|stop|status}"
    exit 1
    ;;
esac
