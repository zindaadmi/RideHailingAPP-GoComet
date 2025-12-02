#!/bin/bash
# Quick test script to generate traffic for New Relic monitoring

echo "🚀 Testing APIs for New Relic monitoring..."
echo ""

# Test 1: Create multiple ride requests
echo "📝 Creating ride requests..."
for i in {1..5}; do
  echo "  Creating ride $i..."
  curl -s -X POST http://localhost:8080/v1/rides \
    -H "Content-Type: application/json" \
    -d "{
      \"riderId\": \"RIDER-$i\",
      \"pickupLatitude\": 28.7041,
      \"pickupLongitude\": 77.1025,
      \"destinationLatitude\": 28.5355,
      \"destinationLongitude\": 77.3910,
      \"tier\": \"ECONOMY\",
      \"paymentMethod\": \"CARD\",
      \"idempotencyKey\": \"test-$i-$(date +%s)\"
    }" > /dev/null
  sleep 1
done

# Test 2: Get ride status (multiple times)
echo "📊 Fetching ride status..."
for i in {1..10}; do
  curl -s http://localhost:8080/v1/rides/RIDE-1234567890 > /dev/null 2>&1
  sleep 0.5
done

# Test 3: Update driver locations
echo "📍 Updating driver locations..."
for i in {1..10}; do
  curl -s -X POST http://localhost:8080/v1/drivers/DRIVER-1/location \
    -H "Content-Type: application/json" \
    -d "{\"latitude\": 28.7041, \"longitude\": 77.1025}" > /dev/null
  sleep 0.3
done

echo ""
echo "✅ Done! Check New Relic dashboard in 1-2 minutes:"
echo "   https://one.newrelic.com → APM & Services → GoComet DAW"
echo ""

