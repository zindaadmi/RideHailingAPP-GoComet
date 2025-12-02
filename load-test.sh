#!/bin/bash

# Load Testing Script for GoComet DAW APIs
# Generates 15,000+ requests across all endpoints

API_BASE="http://localhost:8080/v1"
TOTAL_REQUESTS=0
SUCCESS=0
FAILED=0

echo "╔══════════════════════════════════════════════════════════════╗"
echo "║     GoComet DAW - Load Testing (15,000+ requests)          ║"
echo "╚══════════════════════════════════════════════════════════════╝"
echo ""
echo "🚀 Starting load test..."
echo "📊 Target: 15,000+ requests across all APIs"
echo ""

# Function to make request and count
make_request() {
    local endpoint=$1
    local method=$2
    local data=$3
    local description=$4
    
    if [ "$method" = "POST" ]; then
        response=$(curl -s -w "\n%{http_code}" -X POST "$API_BASE$endpoint" \
            -H "Content-Type: application/json" \
            -d "$data" 2>/dev/null)
    else
        response=$(curl -s -w "\n%{http_code}" "$API_BASE$endpoint" 2>/dev/null)
    fi
    
    http_code=$(echo "$response" | tail -n1)
    TOTAL_REQUESTS=$((TOTAL_REQUESTS + 1))
    
    if [ "$http_code" -ge 200 ] && [ "$http_code" -lt 300 ]; then
        SUCCESS=$((SUCCESS + 1))
        echo -n "✅"
    else
        FAILED=$((FAILED + 1))
        echo -n "❌"
    fi
    
    # Print progress every 100 requests
    if [ $((TOTAL_REQUESTS % 100)) -eq 0 ]; then
        echo " [$TOTAL_REQUESTS requests] Success: $SUCCESS, Failed: $FAILED"
    fi
}

# Create initial rides for testing
echo "📝 Phase 1: Creating initial rides (500 requests)..."
RIDE_IDS=()
for i in $(seq 1 500); do
    ride_data="{
        \"riderId\": \"RIDER-$i\",
        \"pickupLatitude\": $(awk "BEGIN {print 28.7041 + (rand() * 0.1)}"),
        \"pickupLongitude\": $(awk "BEGIN {print 77.1025 + (rand() * 0.1)}"),
        \"destinationLatitude\": $(awk "BEGIN {print 28.5355 + (rand() * 0.1)}"),
        \"destinationLongitude\": $(awk "BEGIN {print 77.3910 + (rand() * 0.1)}"),
        \"tier\": \"ECONOMY\",
        \"paymentMethod\": \"CARD\",
        \"idempotencyKey\": \"load-test-$i-$(date +%s)\"
    }"
    
    response=$(curl -s -X POST "$API_BASE/rides" \
        -H "Content-Type: application/json" \
        -d "$ride_data" 2>/dev/null)
    
    ride_id=$(echo "$response" | grep -o '"rideId":"[^"]*"' | cut -d'"' -f4)
    if [ ! -z "$ride_id" ]; then
        RIDE_IDS+=("$ride_id")
    fi
    
    TOTAL_REQUESTS=$((TOTAL_REQUESTS + 1))
    if [ $((i % 50)) -eq 0 ]; then
        echo "  Created $i rides... [Total requests: $TOTAL_REQUESTS]"
    fi
done
echo "✅ Phase 1 complete: Created ${#RIDE_IDS[@]} rides"
echo ""

# Phase 2: GET /v1/rides/{id} - 3,000 requests
echo "📊 Phase 2: Testing GET /v1/rides/{id} (3,000 requests)..."
for i in $(seq 1 3000); do
    if [ ${#RIDE_IDS[@]} -gt 0 ]; then
        ride_id=${RIDE_IDS[$((i % ${#RIDE_IDS[@]}))]}
        make_request "/rides/$ride_id" "GET" "" "GET ride status"
    else
        make_request "/rides/RIDE-1234567890" "GET" "" "GET ride status"
    fi
done
echo ""
echo "✅ Phase 2 complete"
echo ""

# Phase 3: POST /v1/drivers/{id}/location - 3,000 requests
echo "📍 Phase 3: Testing POST /v1/drivers/{id}/location (3,000 requests)..."
DRIVERS=("DRIVER-1" "DRIVER-2" "DRIVER-3" "DRIVER-4" "DRIVER-5")
for i in $(seq 1 3000); do
    driver=${DRIVERS[$((i % ${#DRIVERS[@]}))]}
    location_data="{
        \"latitude\": $(awk "BEGIN {print 28.7041 + (rand() * 0.1)}"),
        \"longitude\": $(awk "BEGIN {print 77.1025 + (rand() * 0.1)}")
    }"
    make_request "/drivers/$driver/location" "POST" "$location_data" "Update driver location"
done
echo ""
echo "✅ Phase 3 complete"
echo ""

# Phase 4: POST /v1/drivers/{id}/accept - 2,000 requests
echo "✅ Phase 4: Testing POST /v1/drivers/{id}/accept (2,000 requests)..."
for i in $(seq 1 2000); do
    driver=${DRIVERS[$((i % ${#DRIVERS[@]}))]}
    ride_num=$((i % ${#RIDE_IDS[@]} + 1))
    make_request "/drivers/$driver/accept?rideId=$ride_num" "POST" "" "Accept ride"
done
echo ""
echo "✅ Phase 4 complete"
echo ""

# Phase 5: GET /v1/trips/{id} - 2,000 requests
echo "🚗 Phase 5: Testing GET /v1/trips/{id} (2,000 requests)..."
for i in $(seq 1 2000); do
    trip_id=$((i % 100 + 1))
    make_request "/trips/$trip_id" "GET" "" "Get trip"
done
echo ""
echo "✅ Phase 5 complete"
echo ""

# Phase 6: POST /v1/trips/{id}/end - 1,500 requests
echo "🏁 Phase 6: Testing POST /v1/trips/{id}/end (1,500 requests)..."
for i in $(seq 1 1500); do
    trip_id=$((i % 100 + 1))
    make_request "/trips/$trip_id/end?endLatitude=28.5355&endLongitude=77.3910" "POST" "" "End trip"
done
echo ""
echo "✅ Phase 6 complete"
echo ""

# Phase 7: POST /v1/payments - 1,500 requests
echo "💳 Phase 7: Testing POST /v1/payments (1,500 requests)..."
for i in $(seq 1 1500); do
    trip_id=$((i % 100 + 1))
    payment_data="{
        \"tripId\": $trip_id,
        \"idempotencyKey\": \"payment-load-test-$i-$(date +%s)\"
    }"
    make_request "/payments" "POST" "$payment_data" "Process payment"
done
echo ""
echo "✅ Phase 7 complete"
echo ""

# Phase 8: GET /v1/payments/{id} - 1,000 requests
echo "💰 Phase 8: Testing GET /v1/payments/{id} (1,000 requests)..."
for i in $(seq 1 1000); do
    payment_id="PAY-$i"
    make_request "/payments/$payment_id" "GET" "" "Get payment"
done
echo ""
echo "✅ Phase 8 complete"
echo ""

# Phase 9: GET /v1/drivers/{id} - 1,000 requests
echo "👤 Phase 9: Testing GET /v1/drivers/{id} (1,000 requests)..."
for i in $(seq 1 1000); do
    driver=${DRIVERS[$((i % ${#DRIVERS[@]}))]}
    make_request "/drivers/$driver" "GET" "" "Get driver"
done
echo ""
echo "✅ Phase 9 complete"
echo ""

# Final summary
echo ""
echo "╔══════════════════════════════════════════════════════════════╗"
echo "║                    LOAD TEST SUMMARY                          ║"
echo "╚══════════════════════════════════════════════════════════════╝"
echo ""
echo "📊 Total Requests: $TOTAL_REQUESTS"
echo "✅ Successful: $SUCCESS"
echo "❌ Failed: $FAILED"
echo "📈 Success Rate: $(awk "BEGIN {printf \"%.2f\", ($SUCCESS/$TOTAL_REQUESTS)*100}")%"
echo ""
echo "🎯 Test Distribution:"
echo "   - POST /v1/rides: 500 requests"
echo "   - GET /v1/rides/{id}: 3,000 requests"
echo "   - POST /v1/drivers/{id}/location: 3,000 requests"
echo "   - POST /v1/drivers/{id}/accept: 2,000 requests"
echo "   - GET /v1/trips/{id}: 2,000 requests"
echo "   - POST /v1/trips/{id}/end: 1,500 requests"
echo "   - POST /v1/payments: 1,500 requests"
echo "   - GET /v1/payments/{id}: 1,000 requests"
echo "   - GET /v1/drivers/{id}: 1,000 requests"
echo ""
echo "✅ Load test complete! Check New Relic dashboard for performance metrics."
echo ""

