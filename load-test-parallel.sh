#!/bin/bash

# Parallel Load Testing Script - Faster execution
# Generates 15,000+ requests using parallel processing

API_BASE="http://localhost:8080/v1"
CONCURRENT_REQUESTS=50  # Number of parallel requests
TOTAL_REQUESTS=15000

echo "╔══════════════════════════════════════════════════════════════╗"
echo "║  GoComet DAW - Parallel Load Testing (15,000+ requests)    ║"
echo "╚══════════════════════════════════════════════════════════════╝"
echo ""
echo "🚀 Starting parallel load test..."
echo "📊 Target: $TOTAL_REQUESTS requests"
echo "⚡ Concurrent: $CONCURRENT_REQUESTS parallel requests"
echo ""

# Function to test an endpoint
test_endpoint() {
    local endpoint=$1
    local method=$2
    local data=$3
    local count=$4
    
    for i in $(seq 1 $count); do
        if [ "$method" = "POST" ]; then
            curl -s -X POST "$API_BASE$endpoint" \
                -H "Content-Type: application/json" \
                -d "$data" > /dev/null 2>&1
        else
            curl -s "$API_BASE$endpoint" > /dev/null 2>&1
        fi
        echo -n "."
    done
}

# Create initial rides
echo "📝 Creating 500 initial rides..."
for i in $(seq 1 500); do
    curl -s -X POST "$API_BASE/rides" \
        -H "Content-Type: application/json" \
        -d "{
            \"riderId\": \"RIDER-$i\",
            \"pickupLatitude\": 28.7041,
            \"pickupLongitude\": 77.1025,
            \"destinationLatitude\": 28.5355,
            \"destinationLongitude\": 77.3910,
            \"tier\": \"ECONOMY\",
            \"paymentMethod\": \"CARD\",
            \"idempotencyKey\": \"load-$i-$(date +%s)\"
        }" > /dev/null 2>&1
done
echo "✅ Created 500 rides"
echo ""

# Run tests in parallel
echo "🔥 Running parallel load tests..."
echo ""

# Test 1: GET /v1/rides/{id} - 3,000 requests
echo "📊 Testing GET /v1/rides/{id} (3,000 requests)..."
for i in $(seq 1 $CONCURRENT_REQUESTS); do
    (test_endpoint "/rides/RIDE-1234567890" "GET" "" 60) &
done
wait
echo " ✅ Complete"
echo ""

# Test 2: POST /v1/drivers/{id}/location - 3,000 requests
echo "📍 Testing POST /v1/drivers/{id}/location (3,000 requests)..."
for i in $(seq 1 $CONCURRENT_REQUESTS); do
    (test_endpoint "/drivers/DRIVER-1/location" "POST" '{"latitude":28.7041,"longitude":77.1025}' 60) &
done
wait
echo " ✅ Complete"
echo ""

# Test 3: POST /v1/drivers/{id}/accept - 2,000 requests
echo "✅ Testing POST /v1/drivers/{id}/accept (2,000 requests)..."
for i in $(seq 1 $CONCURRENT_REQUESTS); do
    (test_endpoint "/drivers/DRIVER-1/accept?rideId=$i" "POST" "" 40) &
done
wait
echo " ✅ Complete"
echo ""

# Test 4: GET /v1/trips/{id} - 2,000 requests
echo "🚗 Testing GET /v1/trips/{id} (2,000 requests)..."
for i in $(seq 1 $CONCURRENT_REQUESTS); do
    (test_endpoint "/trips/$i" "GET" "" 40) &
done
wait
echo " ✅ Complete"
echo ""

# Test 5: POST /v1/trips/{id}/end - 1,500 requests
echo "🏁 Testing POST /v1/trips/{id}/end (1,500 requests)..."
for i in $(seq 1 $CONCURRENT_REQUESTS); do
    (test_endpoint "/trips/$i/end?endLatitude=28.5355&endLongitude=77.3910" "POST" "" 30) &
done
wait
echo " ✅ Complete"
echo ""

# Test 6: POST /v1/payments - 1,500 requests
echo "💳 Testing POST /v1/payments (1,500 requests)..."
for i in $(seq 1 $CONCURRENT_REQUESTS); do
    (test_endpoint "/payments" "POST" "{\"tripId\":$i,\"idempotencyKey\":\"pay-$i\"}" 30) &
done
wait
echo " ✅ Complete"
echo ""

# Test 7: GET /v1/payments/{id} - 1,000 requests
echo "💰 Testing GET /v1/payments/{id} (1,000 requests)..."
for i in $(seq 1 $CONCURRENT_REQUESTS); do
    (test_endpoint "/payments/PAY-$i" "GET" "" 20) &
done
wait
echo " ✅ Complete"
echo ""

# Test 8: GET /v1/drivers/{id} - 1,000 requests
echo "👤 Testing GET /v1/drivers/{id} (1,000 requests)..."
for i in $(seq 1 $CONCURRENT_REQUESTS); do
    (test_endpoint "/drivers/DRIVER-1" "GET" "" 20) &
done
wait
echo " ✅ Complete"
echo ""

echo ""
echo "╔══════════════════════════════════════════════════════════════╗"
echo "║                    LOAD TEST COMPLETE                        ║"
echo "╚══════════════════════════════════════════════════════════════╝"
echo ""
echo "✅ Total: ~15,000+ requests generated"
echo "📊 Check New Relic dashboard for performance metrics"
echo ""

