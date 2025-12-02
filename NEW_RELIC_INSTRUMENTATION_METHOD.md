# New Relic Instrumentation Method for Java

## Don't Manually Add - It Should Auto-Detect!

**IMPORTANT:** You don't need to manually add the service. New Relic will automatically detect your Java application once:
1. ✅ Java agent is running
2. ✅ Application generates traffic
3. ✅ Wait 2-3 minutes

---

## But If You Want to Proceed Manually...

### Choose: **"Java Agent (APM)"** or **"APM"**

When New Relic asks for instrumentation method, select:

**Option 1: "Java Agent" or "APM"**
- This is for Application Performance Monitoring
- Uses the Java agent JAR file
- Best for Spring Boot applications

**Option 2: "Manual Installation"**
- If you see this, it will give you instructions
- Follow the steps to configure the agent

**DO NOT choose:**
- ❌ "Infrastructure Agent" (that's for servers)
- ❌ "Kubernetes" (unless you're using K8s)
- ❌ "Docker" (unless containerized)

---

## Manual Setup Steps (If Needed)

### Step 1: Choose Instrumentation Method

Select: **"Java Agent (APM)"** or **"APM"**

### Step 2: Follow the Instructions

New Relic will show you:
1. Download the Java agent
2. Configure `newrelic.yml`
3. Add JVM argument: `-javaagent:newrelic-agent-8.10.0.jar`

### Step 3: Use Your Existing Setup

**You already have everything set up!**

```bash
# Your configuration is already done:
# ✅ newrelic-agent-8.10.0.jar (downloaded)
# ✅ newrelic.yml (configured)
# ✅ License key (in config)

# Just start with:
./gradlew bootRun -Pnewrelic.agent.jar.path=./newrelic-agent-8.10.0.jar
```

---

## Why Auto-Detection is Better

**Auto-detection:**
- ✅ Simpler - no manual setup needed
- ✅ Automatic - appears when agent connects
- ✅ Less configuration

**Manual setup:**
- ⚠️ More steps
- ⚠️ May require additional configuration
- ⚠️ Not necessary if agent is working

---

## Recommended: Wait for Auto-Detection

### Step 1: Make Sure App is Running with Agent

```bash
# Stop current app (if running without agent)
# Then start with:
./gradlew bootRun -Pnewrelic.agent.jar.path=./newrelic-agent-8.10.0.jar
```

### Step 2: Generate Traffic

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

# Make more requests
for i in {1..20}; do
  curl -s http://localhost:8080/v1/rides/RIDE-1234567890 > /dev/null
  sleep 0.3
done
```

### Step 3: Wait and Check

1. **Wait 2-3 minutes**
2. Go to: **https://one.eu.newrelic.com**
3. Click: **APM & Services**
4. **Refresh** the page
5. **"GoComet DAW"** should appear automatically

---

## If You Still Want Manual Setup

### Choose: "Java Agent (APM)"

Then provide:
- **Application Name:** `GoComet DAW`
- **License Key:** (from your newrelic.yml)
- **Agent Location:** `/Users/nikhilparakh/Downloads/GoComet-DAW/newrelic-agent-8.10.0.jar`

But again, **this is not necessary** - auto-detection should work!

---

## Troubleshooting

### Service Still Not Appearing?

1. **Check agent is running:**
   ```bash
   # Look for in application logs:
   [New Relic] Agent connected successfully
   ```

2. **Check license key:**
   ```bash
   grep license_key newrelic.yml
   # Should be your EU license key (starts with eu01)
   ```

3. **Generate more traffic:**
   ```bash
   # Run load test
   ./test-newrelic.sh
   ```

4. **Check New Relic logs:**
   ```bash
   tail -50 logs/newrelic_agent.log
   ```

---

## Summary

**Best Approach:**
1. ✅ Don't manually add
2. ✅ Start app with Java agent
3. ✅ Generate traffic
4. ✅ Wait 2-3 minutes
5. ✅ Service appears automatically

**If Manual Setup Required:**
- Choose: **"Java Agent (APM)"**
- But you already have everything configured!

**The service should auto-detect - no manual setup needed!** 🚀

