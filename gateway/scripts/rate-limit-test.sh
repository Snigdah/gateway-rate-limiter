#!/bin/bash

URL="http://localhost:8080/api/users/test"

echo "ULTIMATE RATE LIMIT TEST - Should see HTTP 429"
echo "=============================================="
echo "Using API Key: clientId1"
echo ""

# Phase 1: Burst requests to exceed limit
echo "PHASE 1: Sending 60 rapid requests (exceeds 40 burst capacity)..."
echo ""

success_count=0
rate_limit_count=0

for i in {1..60}; do
    response=$(curl -s -w "|%{http_code}" --header "X-API-Key: clientId1" "$URL")
    status=$(echo "$response" | cut -d'|' -f2)

    timestamp=$(date +"%H:%M:%S.%3N")

    if [ "$status" -eq 429 ]; then
        echo -e "$timestamp - Request $i: \e[31mHTTP 429 ❌ RATE LIMITED\e[0m"
        ((rate_limit_count++))
    elif [ "$status" -eq 200 ]; then
        echo -e "$timestamp - Request $i: \e[32mHTTP 200 ✅ ALLOWED\e[0m"
        ((success_count++))
    else
        echo -e "$timestamp - Request $i: \e[33mHTTP $status ⚠️ OTHER\e[0m"
    fi
done

echo ""
echo "=============================================="
echo -e "PHASE 1 RESULTS:"
echo -e "  ✅ ALLOWED: $success_count requests"
echo -e "  ❌ RATE LIMITED: $rate_limit_count requests"
echo ""

# Phase 2: Immediate follow-up test
echo "PHASE 2: Immediate follow-up (should see more 429s)..."
echo ""

for i in {61..70}; do
    status=$(curl -s -o /dev/null -w "%{http_code}" --header "X-API-Key: clientId1" "$URL")
    timestamp=$(date +"%H:%M:%S.%3N")

    if [ "$status" -eq 429 ]; then
        echo -e "$timestamp - Request $i: \e[31mHTTP 429 ❌ RATE LIMITED\e[0m"
    else
        echo -e "$timestamp - Request $i: \e[32mHTTP 200 ✅ ALLOWED\e[0m"
    fi
done

echo ""
echo "=============================================="
echo "EXPECTED BEHAVIOR:"
echo "- First ~40 requests: ✅ ALLOWED"
echo "- Remaining requests: ❌ RATE LIMITED"
echo ""
echo "If you don't see 429 responses, check:"
echo "1. Redis is running: docker run -d -p 6379:6379 redis"
echo "2. Spring Boot logs for Redis connection errors"
echo "3. All dependencies are included"
