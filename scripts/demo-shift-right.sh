#!/bin/bash
# ============================================================
# Shift-Right Testing - End-to-End Demonstration
# ============================================================

EC2_IP="${1:-localhost}"
UNLEASH_URL="http://${EC2_IP}:4242"

echo "============================================"
echo " Shift-Right Testing Demo"
echo " Target: http://${EC2_IP}"
echo "============================================"

echo ""
echo "Step 1: Verify both backends are healthy"
echo "─────────────────────────────────────────"
echo "V1 health:"
curl -s "http://${EC2_IP}/v1/actuator/health" | python3 -m json.tool 2>/dev/null || curl -s "http://${EC2_IP}/v1/actuator/health" || echo "FAILED"
echo ""
echo "V2 health:"
curl -s "http://${EC2_IP}/v2/actuator/health" | python3 -m json.tool 2>/dev/null || curl -s "http://${EC2_IP}/v2/actuator/health" || echo "FAILED"

echo ""
echo "Step 2: Verify canary routing (send 20 requests, check distribution)"
echo "─────────────────────────────────────────────────────────────────────"
v1=0
v2=0
for i in $(seq 1 20); do
    version=$(curl -s -D - -o /dev/null -H "Content-Type: application/json" \
        -X POST -d '{"email":"test@test.com","password":"pass"}' \
        "http://${EC2_IP}/api/auth/login" 2>/dev/null | grep -i "^x-backend-version:" | cut -d: -f2 | tr -d ' \r\n')

    if [ "$version" = "v1" ]; then
        v1=$((v1 + 1))
    elif [ "$version" = "v2" ]; then
        v2=$((v2 + 1))
    fi
done
echo "V1 handled: $v1 requests"
echo "V2 handled: $v2 requests"
echo "Expected: ~18 V1, ~2 V2"

echo ""
echo "Step 3: Everything healthy. Now enable failure injection in Unleash."
echo "Open Unleash UI at: ${UNLEASH_URL}"
echo "Enable the 'simulate-failure' feature flag."
if [ -t 0 ]; then
    echo "Press Enter when done..."
    read -r
else
    echo "(Running non-interactively; proceeding)"
fi

echo ""
echo "Step 4: Send requests and observe V2 failures"
echo "──────────────────────────────────────────────"
errors=0
total=20
for i in $(seq 1 $total); do
    code=$(curl -s -o /dev/null -w '%{http_code}' -H "Content-Type: application/json" \
        -X POST -d '{"email":"test@test.com","password":"pass"}' \
        "http://${EC2_IP}/api/auth/login")
    if [ "$code" -ge 500 ]; then
        errors=$((errors + 1))
        echo "  Request $i: HTTP $code (V2 failure)"
    fi
done
echo "Errors: $errors / $total"

echo ""
echo "Step 5: Start the rollback controller in another terminal:"
echo "  ssh into EC2 and run: cd ~/StudentManagement && bash scripts/rollback-controller.sh"
echo ""
echo "Demo complete. The rollback controller will automatically remove V2 when it detects failures."
