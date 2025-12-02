# New Relic Testing Guide - Quick Start

## Step 1: Update License Key

**IMPORTANT:** Replace the license key in `newrelic.yml` with your actual key:

```yaml
license_key: YOUR_ACTUAL_LICENSE_KEY_HERE
```

The file is located at: `newrelic.yml` (root directory)

## Step 2: Download New Relic Agent

```bash
cd /Users/nikhilparakh/Downloads/GoComet-DAW

# Download the agent
curl -O https://download.newrelic.com/newrelic/java-agent/newrelic-agent/8.10.0/newrelic-agent-8.10.0.jar
```

Or manually download from:
https://download.newrelic.com/newrelic/java-agent/newrelic-agent/8.10.0/newrelic-agent-8.10.0.jar

## Step 3: Run Application with New Relic

```bash
./gradlew bootRun -Pnewrelic.agent.jar.path=./newrelic-agent-8.10.0.jar
```

**Wait for:** Application to start (you'll see "Started GoCometDawApplication")

## Step 4: Generate Load/Test the APIs

Once the app is running, open a **new terminal** and run these commands to generate traffic:

```bash
# Test 1: Create multiple ride requests
for i in {1..10}; do
  curl -X POST http://localhost:8080/v1/rides \
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
    }"
  sleep 1
done

# Test 2: Get ride status (multiple times)
for i in {1..20}; do
  curl http://localhost:8080/v1/rides/RIDE-1234567890 2>/dev/null
  sleep 0.5
done

# Test 3: Update driver locations
for i in {1..15}; do
  curl -X POST http://localhost:8080/v1/drivers/DRIVER-1/location \
    -H "Content-Type: application/json" \
    -d "{\"latitude\": 28.7041, \"longitude\": 77.1025}"
  sleep 0.3
done
```

**Or use the frontend:**
1. Open `http://localhost:8080` in your browser
2. Create multiple ride requests
3. Update driver locations
4. Let it run for 5-10 minutes to generate data

## Step 5: Access New Relic Dashboard

### Option A: Direct Link
1. Go to: **https://one.newrelic.com**
2. Login with your New Relic account
3. Click on **"APM & Services"** in the left menu
4. Find **"GoComet DAW"** in the application list
5. Click on it to see the dashboard

### Option B: Navigation
1. Login to New Relic: https://one.newrelic.com
2. Click **"APM & Services"** → **"Applications"**
3. Select **"GoComet DAW"**
4. You'll see the main dashboard

## Step 6: What to Screenshot

### Screenshot 1: Overview Dashboard
- **Location:** Main application page
- **What to capture:**
  - Response time graph
  - Throughput (requests/min)
  - Error rate
  - Apdex score

### Screenshot 2: API Performance
- **Location:** Click on "Transactions" tab
- **What to capture:**
  - List of all API endpoints
  - Response times for each endpoint
  - p95, p99 latencies
  - Throughput per endpoint

### Screenshot 3: Database Performance
- **Location:** Click on "Databases" tab
- **What to capture:**
  - Slow queries
  - Query execution times
  - Database response times

### Screenshot 4: Error Analysis
- **Location:** Click on "Errors" tab
- **What to capture:**
  - Error rate
  - Error types
  - Error frequency

### Screenshot 5: Performance Breakdown
- **Location:** Click on a specific transaction (e.g., "POST /v1/rides")
- **What to capture:**
  - Time breakdown (Database, External, Application)
  - Slow query details
  - Transaction trace

## Step 7: Wait for Data

**Important:** New Relic takes 1-2 minutes to show data after the first request.

1. Run the application
2. Generate traffic (use the curl commands or frontend)
3. Wait 2-3 minutes
4. Refresh the New Relic dashboard
5. Data should appear

## Troubleshooting

### No data showing?
1. Check if license key is correct in `newrelic.yml`
2. Verify agent JAR is in the project root
3. Check application logs for New Relic errors
4. Wait 2-3 minutes for data to appear

### Agent not loading?
```bash
# Check if agent JAR exists
ls -la newrelic-agent-8.10.0.jar

# Verify the path in the command
./gradlew bootRun -Pnewrelic.agent.jar.path=./newrelic-agent-8.10.0.jar
```

### Still not working?
Check the logs directory:
```bash
ls -la logs/
cat logs/newrelic_agent.log
```

## Quick Test Script

Save this as `test-newrelic.sh`:

```bash
#!/bin/bash
echo "Testing APIs for New Relic monitoring..."

# Create rides
for i in {1..5}; do
  echo "Creating ride $i..."
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
      \"idempotencyKey\": \"test-$i\"
    }" > /dev/null
  sleep 1
done

echo "Done! Check New Relic dashboard in 1-2 minutes."
```

Run it:
```bash
chmod +x test-newrelic.sh
./test-newrelic.sh
```

---

## Summary

1. ✅ Update license key in `newrelic.yml`
2. ✅ Download agent JAR
3. ✅ Run app with: `./gradlew bootRun -Pnewrelic.agent.jar.path=./newrelic-agent-8.10.0.jar`
4. ✅ Generate traffic (curl or frontend)
5. ✅ Open: https://one.newrelic.com → APM & Services → GoComet DAW
6. ✅ Wait 2-3 minutes for data
7. ✅ Take screenshots of:
   - Overview dashboard
   - API performance
   - Database queries
   - Error rates
   - Transaction breakdown

**That's it!** 🚀

