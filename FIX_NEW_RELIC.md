# Fix: App Not Showing in New Relic

## The Problem
You see "System" in New Relic but not "GoComet DAW" application.

## The Solution

### Step 1: Stop Current Application

**If app is running in a terminal:**
- Press `Ctrl + C` to stop it

**Or kill the process:**
```bash
pkill -f GoCometDawApplication
```

### Step 2: Verify License Key

**IMPORTANT:** Make sure your license key is correct in `newrelic.yml`

1. Go to: https://one.newrelic.com
2. Click: **Account Settings** (top right)
3. Click: **API Keys** or **License Key**
4. Copy your **actual license key**

5. Update `newrelic.yml` line 3:
```yaml
license_key: YOUR_ACTUAL_LICENSE_KEY_HERE
```

**Current key in file:** `eu01xxf4dbb745300ae0460fa4951fd4FFFFNRAL`  
**Replace with:** Your actual key from New Relic account

### Step 3: Start Application WITH New Relic Agent

```bash
cd /Users/nikhilparakh/Downloads/GoComet-DAW
./gradlew bootRun -Pnewrelic.agent.jar.path=./newrelic-agent-8.10.0.jar
```

**Look for in the startup logs:**
```
[New Relic] Agent is starting...
[New Relic] Agent connected successfully
```

### Step 4: Generate Traffic

**Open a NEW terminal** and run:

```bash
# Quick test - create a ride
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

# Make a few more requests
for i in {1..10}; do
  curl -s http://localhost:8080/v1/rides/RIDE-1234567890 > /dev/null
  sleep 0.5
done
```

### Step 5: Wait and Check New Relic

1. **Wait 2-3 minutes** (New Relic needs time to show data)
2. Go to: https://one.newrelic.com
3. Click: **APM & Services** (left sidebar)
4. **Refresh the page** (F5 or Cmd+R)
5. Look for: **"GoComet DAW"** in the list

---

## Quick Verification

### Check if Agent is Running:

```bash
# Check logs (after starting app)
ls -la logs/newrelic_agent.log

# View last few lines
tail -20 logs/newrelic_agent.log
```

**You should see:**
- ✅ "Agent connected successfully"
- ✅ "Application GoComet DAW reporting"

**If you see errors:**
- ❌ "Invalid license key" → Update license key
- ❌ "Failed to connect" → Check internet/firewall

---

## Alternative: Check Application Logs

When you start the app, look for New Relic messages:

```bash
./gradlew bootRun -Pnewrelic.agent.jar.path=./newrelic-agent-8.10.0.jar
```

**Good signs:**
```
[New Relic] Agent is starting...
[New Relic] Agent connected successfully
[New Relic] Application GoComet DAW reporting
```

**Bad signs:**
```
[New Relic] Failed to connect
[New Relic] Invalid license key
```

---

## Still Not Working?

### Option 1: Verify License Key Format

Your license key should look like:
- US: `xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxNRAL`
- EU: `eu01xx...` (starts with eu01)

### Option 2: Check New Relic Region

- **US Account:** https://one.newrelic.com
- **EU Account:** https://one.eu.newrelic.com

Make sure you're using the correct URL!

### Option 3: Manual Test

```bash
# Test if app responds
curl http://localhost:8080/v1/rides/RIDE-1234567890

# Generate more traffic
./test-newrelic.sh
```

---

## Expected Timeline

1. **Start app with agent** → Agent connects (30 seconds)
2. **Generate traffic** → Data sent to New Relic (immediate)
3. **Wait 2-3 minutes** → New Relic processes data
4. **Refresh dashboard** → App appears!

---

## Summary

**The most common issue:** App is running WITHOUT the New Relic agent.

**Fix:**
1. ✅ Stop current app
2. ✅ Update license key (if needed)
3. ✅ Start with: `./gradlew bootRun -Pnewrelic.agent.jar.path=./newrelic-agent-8.10.0.jar`
4. ✅ Generate traffic
5. ✅ Wait 2-3 minutes
6. ✅ Check New Relic dashboard

**That's it!** 🚀

