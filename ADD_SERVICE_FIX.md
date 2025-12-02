# Fix: New Relic Asking to "Add a Service"

## What This Means

When New Relic shows "Add a service" or "No services found", it means:
- The New Relic agent hasn't connected yet, OR
- The agent is running but hasn't sent data yet, OR
- The application needs to generate traffic first

## Solution: Don't Manually Add - Let It Auto-Detect

**IMPORTANT:** You don't need to manually add the service. New Relic will automatically detect it once:
1. ✅ Agent is running and connected
2. ✅ Application generates some traffic
3. ✅ Wait 2-3 minutes for data to appear

---

## Step-by-Step Fix

### Step 1: Verify Application is Running WITH Agent

**Check if you started it correctly:**

```bash
# Should be running with this command:
./gradlew bootRun -Pnewrelic.agent.jar.path=./newrelic-agent-8.10.0.jar
```

**Look for in the startup logs:**
```
[New Relic] Agent is starting...
[New Relic] Agent connected successfully
```

**If you don't see these messages:**
- Stop the app (Ctrl+C)
- Restart with the agent command above

---

### Step 2: Verify License Key

**Check your license key in `newrelic.yml`:**

```bash
grep license_key newrelic.yml
```

**Make sure it's YOUR actual license key from:**
- https://one.newrelic.com → Account Settings → API Keys

**If it's wrong:**
1. Get your real license key from New Relic
2. Update line 3 in `newrelic.yml`
3. Restart the application

---

### Step 3: Generate Traffic (CRITICAL!)

**New Relic won't show your app until you make API calls!**

**Option A: Quick Test (Recommended)**
```bash
# Create a ride
curl -X POST http://localhost:8080/v1/rides \
  -H "Content-Type: application/json" \
  -d '{
    "riderId": "RIDER-1",
    "pickupLatitude": 28.7041,
    "pickupLongitude": 77.1025,
    "destinationLatitude": 28.5355,
    "destinationLongitude": 77.3910,
    "tier": "ECONOMY",
    "paymentMethod": "CARD",
    "idempotencyKey": "test-1"
  }'

# Make multiple requests
for i in {1..20}; do
  curl -s http://localhost:8080/v1/rides/RIDE-1234567890 > /dev/null
  sleep 0.5
done
```

**Option B: Use Frontend**
1. Open: http://localhost:8080
2. Create multiple ride requests
3. Update driver locations
4. Let it run for a few minutes

**Option C: Run Load Test**
```bash
./test-newrelic.sh
```

---

### Step 4: Wait and Refresh

1. **Wait 2-3 minutes** after generating traffic
2. **Refresh** the New Relic page (F5 or Cmd+R)
3. **Check:** APM & Services → Should show "GoComet DAW"

---

## What You Should See

### In Application Logs:
```
[New Relic] Agent is starting...
[New Relic] Agent connected successfully
[New Relic] Application GoComet DAW reporting
```

### In New Relic Dashboard:
1. Go to: https://one.newrelic.com
2. Click: **APM & Services**
3. After 2-3 minutes: **"GoComet DAW"** should appear automatically

---

## Troubleshooting

### Still Not Showing?

**Check 1: Agent Logs**
```bash
# Check if logs directory exists
ls -la logs/

# View agent logs
tail -50 logs/newrelic_agent.log
```

**Look for:**
- ✅ "Agent connected successfully"
- ✅ "Application GoComet DAW reporting"
- ❌ "Failed to connect" → Check license key
- ❌ "Invalid license key" → Update license key

**Check 2: License Key Format**

Your license key should be:
- **US Account:** `xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxNRAL` (ends with NRAL)
- **EU Account:** `eu01xx...` (starts with eu01)

**Check 3: New Relic Region**

Make sure you're using the correct URL:
- **US:** https://one.newrelic.com
- **EU:** https://one.eu.newrelic.com

**Check 4: Generate More Traffic**

Sometimes you need more requests:
```bash
# Generate 50+ requests
for i in {1..50}; do
  curl -s -X POST http://localhost:8080/v1/rides \
    -H "Content-Type: application/json" \
    -d "{\"riderId\":\"RIDER-$i\",\"pickupLatitude\":28.7041,\"pickupLongitude\":77.1025,\"destinationLatitude\":28.5355,\"destinationLongitude\":77.3910,\"tier\":\"ECONOMY\",\"paymentMethod\":\"CARD\",\"idempotencyKey\":\"test-$i\"}" > /dev/null
  sleep 0.2
done
```

---

## Common Mistakes

### ❌ Mistake 1: Running App Without Agent
```bash
# WRONG - No agent
./gradlew bootRun

# CORRECT - With agent
./gradlew bootRun -Pnewrelic.agent.jar.path=./newrelic-agent-8.10.0.jar
```

### ❌ Mistake 2: Wrong License Key
- Using example/placeholder key
- Using key from different account
- **Fix:** Get your actual key from New Relic account

### ❌ Mistake 3: Not Generating Traffic
- App running but no API calls
- **Fix:** Make API calls or use frontend

### ❌ Mistake 4: Not Waiting
- Checking immediately after starting
- **Fix:** Wait 2-3 minutes after generating traffic

---

## Quick Verification Script

Run this to check everything:

```bash
#!/bin/bash
echo "=== New Relic Service Detection Check ==="
echo ""
echo "1. App running with agent?"
if pgrep -f "GoCometDawApplication" > /dev/null; then
    echo "   ✅ Yes"
else
    echo "   ❌ No - Start with: ./gradlew bootRun -Pnewrelic.agent.jar.path=./newrelic-agent-8.10.0.jar"
fi
echo ""
echo "2. License key set?"
if grep -q "license_key:" newrelic.yml && ! grep -q "YOUR_LICENSE_KEY" newrelic.yml; then
    echo "   ✅ Yes"
else
    echo "   ❌ No - Update newrelic.yml"
fi
echo ""
echo "3. Generated traffic?"
echo "   Run: curl -X POST http://localhost:8080/v1/rides ..."
echo ""
echo "4. Waited 2-3 minutes?"
echo "   ⏱️  Wait, then refresh New Relic dashboard"
echo ""
echo "5. Check New Relic:"
echo "   https://one.newrelic.com → APM & Services"
```

---

## Summary

**Don't manually add the service!** New Relic will auto-detect it.

**Steps:**
1. ✅ Start app WITH agent
2. ✅ Verify license key
3. ✅ Generate traffic (API calls)
4. ✅ Wait 2-3 minutes
5. ✅ Refresh New Relic dashboard
6. ✅ "GoComet DAW" should appear automatically

**The service will appear automatically once the agent connects and sends data!** 🚀

