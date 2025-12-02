# New Relic Java Agent Setup (Correct Method)

## Important: Java Agent vs CLI

You ran the CLI installer, but for **Java Spring Boot applications**, you need the **Java Agent**, not the CLI.

- **CLI** = Infrastructure monitoring (servers, containers)
- **Java Agent** = Application Performance Monitoring (APM) for Java apps

**For this project, we need the Java Agent!**

---

## Your New Relic Credentials

**Note:** For Java agent, we need the **License Key**, not the API Key.

**To get your credentials:**
- Go to: https://one.eu.newrelic.com → Account Settings
- Copy your **License Key** (starts with `eu01` for EU region)
- Your Account ID and Region can be found in Account Settings

---

## Step 1: Get Your License Key

1. Go to: **https://one.eu.newrelic.com** (EU region)
2. Login with your account
3. Click: **Account Settings** (top right)
4. Click: **API Keys** or **License Key**
5. Copy your **License Key** (different from API Key)

**License Key format:**
- EU: `eu01xx...` (starts with `eu01`)
- US: `xxxxxxxx...NRAL` (ends with `NRAL`)

---

## Step 2: Update newrelic.yml

Update the license key in `newrelic.yml`:

```yaml
common: &default_settings
  app_name: GoComet DAW
  license_key: YOUR_EU_LICENSE_KEY_HERE  # ← Update this
```

**Since you're in EU region, your license key should start with `eu01`**

---

## Step 3: Verify Java Agent is Downloaded

```bash
cd /Users/nikhilparakh/Downloads/GoComet-DAW
ls -lh newrelic-agent-8.10.0.jar
```

**If missing, download it:**
```bash
curl -L -o newrelic-agent-8.10.0.jar \
  https://download.newrelic.com/newrelic/java-agent/newrelic-agent/8.10.0/newrelic-agent-8.10.0.jar
```

---

## Step 4: Start Application with Java Agent

```bash
cd /Users/nikhilparakh/Downloads/GoComet-DAW

# Start with Java agent
./gradlew bootRun -Pnewrelic.agent.jar.path=./newrelic-agent-8.10.0.jar
```

**Look for in logs:**
```
[New Relic] Agent is starting...
[New Relic] Agent connected successfully
```

---

## Step 5: Access New Relic Dashboard (EU Region)

Since you're in EU region, use:

**EU Dashboard:** https://one.eu.newrelic.com

1. Go to: https://one.eu.newrelic.com
2. Login
3. Click: **APM & Services**
4. Look for: **"GoComet DAW"**

**Important:** Use the EU URL, not the US one!

---

## Step 6: Generate Traffic

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

---

## Step 7: Wait and Check

1. **Wait 2-3 minutes** after generating traffic
2. Go to: **https://one.eu.newrelic.com**
3. Click: **APM & Services**
4. **Refresh** the page
5. **"GoComet DAW"** should appear

---

## Troubleshooting

### Issue: Wrong Region URL

**Problem:** Using US URL but account is EU

**Solution:** Use https://one.eu.newrelic.com (not .com)

### Issue: License Key vs API Key

**Problem:** Using API Key instead of License Key

**Solution:** 
- API Key: `NRAK-...` (for CLI/API calls)
- License Key: `eu01xx...` or `...NRAL` (for Java agent)

Get License Key from: Account Settings → License Key

### Issue: Agent Not Connecting

**Check logs:**
```bash
tail -50 logs/newrelic_agent.log
```

**Look for:**
- ✅ "Agent connected successfully"
- ❌ "Invalid license key" → Update license key
- ❌ "Failed to connect" → Check internet/firewall

---

## Quick Setup Checklist

- [ ] Get License Key from https://one.eu.newrelic.com
- [ ] Update `newrelic.yml` with EU license key
- [ ] Verify `newrelic-agent-8.10.0.jar` exists
- [ ] Start app: `./gradlew bootRun -Pnewrelic.agent.jar.path=./newrelic-agent-8.10.0.jar`
- [ ] Generate traffic (API calls)
- [ ] Wait 2-3 minutes
- [ ] Check: https://one.eu.newrelic.com → APM & Services

---

## Summary

1. **You installed CLI** (infrastructure monitoring) ✅
2. **But we need Java Agent** (APM for Java apps) ✅
3. **Get License Key** (not API Key) from EU dashboard
4. **Update newrelic.yml** with EU license key
5. **Start app with Java agent**
6. **Use EU dashboard:** https://one.eu.newrelic.com
7. **Generate traffic and wait**

**The Java agent is what will show your Spring Boot app in New Relic!** 🚀

