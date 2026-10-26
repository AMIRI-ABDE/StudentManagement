#!/bin/bash
# ============================================================
# Automatic Canary Rollback Controller
# Monitors backend-v2 and rolls back if error rate is too high
# ============================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# Configuration
NGINX_CONTAINER="student-nginx"
V2_CONTAINER="student-backend-v2"
CHECK_INTERVAL="${CHECK_INTERVAL:-10}"          # seconds between checks
ERROR_THRESHOLD="${ERROR_THRESHOLD:-5}"           # percent error rate to trigger rollback
MIN_REQUESTS="${MIN_REQUESTS:-20}"             # minimum requests before evaluating
CONSECUTIVE_FAILURES="${CONSECUTIVE_FAILURES:-3}"      # consecutive bad checks before rollback
ROLLBACK_NGINX_CONF="$PROJECT_ROOT/nginx/nginx-rollback.conf"
LOG_FILE="$PROJECT_ROOT/rollback.log"

# State
failure_count=0
total_checks=0

log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" | tee -a "$LOG_FILE"
}

check_v2_health() {
    local response
    response=$(docker exec "$V2_CONTAINER" wget -q -O - http://localhost:8080/actuator/health 2>/dev/null) || return 1

    local status
    status=$(echo "$response" | grep -o '"status":"[^"]*"' | head -1 | cut -d'"' -f4)

    if [ "$status" = "UP" ]; then
        return 0
    else
        return 1
    fi
}

check_v2_error_rate() {
    # Send test requests to V2 directly and check for errors
    local errors=0
    local total=5

    for i in $(seq 1 $total); do
        local http_code
        http_code=$(docker exec "$V2_CONTAINER" wget --server-response -q -O /dev/null http://localhost:8080/api/auth/health 2>&1 | grep "HTTP/" | tail -1 | awk '{print $2}') || http_code="500"

        if [ "$http_code" -ge 500 ] 2>/dev/null; then
            errors=$((errors + 1))
        fi
    done

    local rate=$((errors * 100 / total))
    echo "$rate"
}

do_rollback() {
    log "ROLLBACK TRIGGERED: Removing V2 from Nginx routing"

    # Copy rollback config to nginx container
    docker cp "$ROLLBACK_NGINX_CONF" "$NGINX_CONTAINER":/etc/nginx/conf.d/default.conf

    # Test nginx config
    if docker exec "$NGINX_CONTAINER" nginx -t 2>/dev/null; then
        # Reload nginx
        docker exec "$NGINX_CONTAINER" nginx -s reload
        log "ROLLBACK COMPLETE: All traffic now routed to V1"
        log "To restore canary: docker cp $PROJECT_ROOT/nginx/nginx.conf $NGINX_CONTAINER:/etc/nginx/conf.d/default.conf && docker exec $NGINX_CONTAINER nginx -s reload"
    else
        log "ERROR: Nginx config validation failed. Rollback aborted."
        return 1
    fi
}

# ── Main Loop ──
log "Rollback controller started"
log "Config: threshold=${ERROR_THRESHOLD}%, min_requests=${MIN_REQUESTS}, consecutive=${CONSECUTIVE_FAILURES}"

while true; do
    total_checks=$((total_checks + 1))

    # Check V2 health and error rate
    if ! check_v2_health; then
        failure_count=$((failure_count + 1))
        log "WARNING: V2 health check failed (${failure_count}/${CONSECUTIVE_FAILURES})"
    else
        rate=$(check_v2_error_rate)
        if [ "$rate" -ge "$ERROR_THRESHOLD" ]; then
            failure_count=$((failure_count + 1))
            log "WARNING: V2 error rate high: ${rate}% (${failure_count}/${CONSECUTIVE_FAILURES})"
        else
            # V2 is alive and healthy, reset counter
            failure_count=0
        fi
    fi

    # Trigger rollback after consecutive failures
    if [ "$failure_count" -ge "$CONSECUTIVE_FAILURES" ]; then
        do_rollback
        log "Rollback controller exiting. Manual intervention required to restore canary."
        exit 0
    fi

    sleep "$CHECK_INTERVAL"
done
