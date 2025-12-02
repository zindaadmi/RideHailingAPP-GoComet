# Troubleshooting: App Not Showing in New Relic

## Common Issues and Solutions

### Issue 1: Application Not Running with Agent

**Symptom:** Only "System" shows in New Relic, no "GoComet DAW" application

**Solution:**

1. **Stop the application** (if running without agent)
2. **Start with New Relic agent:**

```bash
cd /Users/nikhilparakh/Downloads/GoComet-DAW
./gradlew bootRun -Pnewrelic.agent.jar.path=./newrelic-agent-8.10.0.jar
```

**Verify:** Look for this in the startup logs:
```
[New Relic] Agent is starting...
```

---

### Issue 2: License Key Not Correct

**Symptom:** Agent starts but no data appears

**Solution:**

1. **Get your license key:**
   - Go to: https://one.newrelic.com
   - Click: **Account Settings** (top right)
   - Click: **API Keys** or **License Key**
   - Copy the license key

2. **Update newrelic.yml:**
   ```yaml
   license_key: YOUR_ACTUAL_LICENSE_KEY_HERE
   ```

3. **Restart the application**

---

### Issue 3: Agent JAR Not Found

**Symptom:** Error about agent JAR path

**Solution:**

```bash
# Download the agent
cd /Users/nikhilparakh/Downloads/GoComet-DAW
curl -L -o newrelic-agent-8.10.0.jar https://download.newrelic.com/newrelic/java-agent/newrelic-agent/8.10.0/newrelic-agent-8.10.0.jar

# Verify it exists
ls -lh newrelic-agent-8.10.0.jar
```

---

### Issue 4: No Data After Starting

**Symptom:** App shows in New Relic but no metrics

**Solution:**

1. **Generate some traffic:**
   ```bash
   # Quick test
   curl -X POST http://localhost:8080/v1/rides \
     -H "Content-Type: application/json" \
     -d '{"riderId":"RIDER-1","pickupLatitude":28.7041,"pickupLongitude":77.1025,"destinationLatitude":28.5355,"destinationLongitude":77.3910,"tier":"ECONOMY","paymentMethod":"CARD","idempotencyKey":"test-1"}'
   ```

2. **Wait 2-3 minutes** - New Relic takes time to show data

3. **Refresh the dashboard**

---

### Issue 5: Wrong Region/Account

**Symptom:** License key works but app doesn't appear

**Solution:**

1. **Check your New Relic region:**
   - US: https://one.newrelic.com
   - EU: https://one.eu.newrelic.com
   - Make sure you're using the correct URL

2. **Verify account:**
   - Make sure you're logged into the correct New Relic account
   - Check if license key matches the account

---

## Step-by-Step Fix

### Step 1: Verify Configuration

```bash
cd /Users/nikhilparakh/Downloads/GoComet-DAW

# Check license key
grep license_key newrelic.yml

# Check agent exists
ls -lh newrelic-agent-8.10.0.jar
```

### Step 2: Stop Any Running Instance

```bash
# Find and kill any running Java process
pkill -f GoCometDawApplication
# Or press Ctrl+C in the terminal running the app
```

### Step 3: Start with Agent

```bash
./gradlew bootRun -Pnewrelic.agent.jar.path=./newrelic-agent-8.10.0.jar
```

**Look for in logs:**
```
[New Relic] Agent is starting...
[New Relic] Agent connected successfully
```

### Step 4: Generate Traffic

```bash
# In a new terminal
curl -X POST http://localhost:8080/v1/rides \
  -H "Content-Type: application/json" \
  -d '{"riderId":"RIDER-1","pickupLatitude":28.7041,"pickupLongitude":77.1025,"destinationLatitude":28.5355,"destinationLongitude":77.3910,"tier":"ECONOMY","paymentMethod":"CARD","idempotencyKey":"test-1"}'
```

### Step 5: Check New Relic

1. Go to: https://one.newrelic.com
2. Click: **APM & Services**
3. Wait 2-3 minutes
4. Look for: **"GoComet DAW"**

---

## Alternative: Check Agent Logs

```bash
# Check if logs directory exists
ls -la logs/

# View New Relic agent logs
tail -f logs/newrelic_agent.log
```

**Look for:**
- ✅ "Agent connected successfully"
- ❌ "Failed to connect" or "Invalid license key"

---

## Quick Verification Script

Run this to check everything:

```bash
#!/bin/bash
echo "=== New Relic Setup Check ==="
echo ""
echo "1. License Key:"
grep license_key newrelic.yml | head -1
echo ""
echo "2. Agent JAR:"
ls -lh newrelic-agent-8.10.0.jar 2>/dev/null || echo "❌ Not found"
echo ""
echo "3. Application running:"
curl -s http://localhost:8080/actuator/health > /dev/null && echo "✅ Running" || echo "❌ Not running"
echo ""
echo "4. New Relic logs:"
if [ -f logs/newrelic_agent.log ]; then
    tail -5 logs/newrelic_agent.log
else
    echo "❌ No logs found"
fi
```

---

## Still Not Working?

### Option 1: Manual Agent Configuration

Add to `application.properties`:

```properties
# New Relic
newrelic.config.file=./newrelic.yml
newrelic.config.app_name=GoComet DAW
```

### Option 2: Use JVM Arguments Directly

```bash
java -javaagent:./newrelic-agent-8.10.0.jar \
     -Dnewrelic.config.file=./newrelic.yml \
     -jar build/libs/GoComet-DAW-0.0.1-SNAPSHOT.jar
```

### Option 3: Check Firewall/Network

- Make sure port 443 is open (New Relic uses HTTPS)
- Check if corporate firewall is blocking
- Try from different network

---

## Expected Behavior

### When Working Correctly:

1. **Application starts** with New Relic agent
2. **Logs show:** "Agent connected successfully"
3. **After 2-3 minutes:** App appears in New Relic dashboard
4. **After generating traffic:** Metrics start appearing

### What You Should See:

- **In New Relic:** "GoComet DAW" in APM & Services list
- **In Logs:** New Relic agent messages
- **In Dashboard:** Response times, throughput, errors

---

## Need More Help?

1. **Check New Relic Status:** https://status.newrelic.com
2. **New Relic Docs:** https://docs.newrelic.com/docs/apm/agents/java-agent/
3. **View Agent Logs:** `logs/newrelic_agent.log`

---

**Most Common Fix:** Make sure you're running the app WITH the agent:
```bash
./gradlew bootRun -Pnewrelic.agent.jar.path=./newrelic-agent-8.10.0.jar
```

