# 🚀 Quick New Relic Testing Steps

## ✅ Step 1: Update License Key (If Needed)

Open `newrelic.yml` and replace the license key on line 3 with YOUR actual license key:

```yaml
license_key: YOUR_ACTUAL_LICENSE_KEY_HERE
```

**Replace with:** Your actual license key from New Relic account (starts with `eu01` for EU region)

---

## ✅ Step 2: Start Application with New Relic

Open Terminal 1 and run:

```bash
cd /Users/nikhilparakh/Downloads/GoComet-DAW
./gradlew bootRun -Pnewrelic.agent.jar.path=./newrelic-agent-8.10.0.jar
```

**Wait for:** `Started GoCometDawApplication` message

---

## ✅ Step 3: Generate Test Traffic

### Option A: Use the Test Script (Easiest)

Open Terminal 2 and run:

```bash
cd /Users/nikhilparakh/Downloads/GoComet-DAW
./test-newrelic.sh
```

### Option B: Use Frontend

1. Open browser: `http://localhost:8080`
2. Create multiple ride requests
3. Update driver locations
4. Let it run for 5-10 minutes

### Option C: Manual API Calls

```bash
# Create rides
curl -X POST http://localhost:8080/v1/rides \
  -H "Content-Type: application/json" \
  -d '{"riderId":"RIDER-1","pickupLatitude":28.7041,"pickupLongitude":77.1025,"destinationLatitude":28.5355,"destinationLongitude":77.3910,"tier":"ECONOMY","paymentMethod":"CARD","idempotencyKey":"test-1"}'
```

---

## ✅ Step 4: Access New Relic Dashboard

1. **Go to:** https://one.newrelic.com
2. **Login** with your New Relic account
3. **Click:** "APM & Services" in left menu
4. **Find:** "GoComet DAW" in the application list
5. **Click** on it to open the dashboard

**Direct link after first data:** https://one.newrelic.com/apm

---

## ✅ Step 5: Wait for Data (IMPORTANT!)

**New Relic takes 1-2 minutes to show data after first request.**

1. Generate traffic (Step 3)
2. **Wait 2-3 minutes**
3. **Refresh** the New Relic dashboard
4. Data should appear!

---

## 📸 Step 6: Screenshots to Take

### Screenshot 1: Overview Dashboard ⭐ (MOST IMPORTANT)
- **Location:** Main application page (first page you see)
- **Capture:**
  - Response time graph
  - Throughput (requests/min)
  - Error rate
  - Apdex score

### Screenshot 2: API Performance
- **Location:** Click "Transactions" tab
- **Capture:**
  - List of all endpoints (POST /v1/rides, GET /v1/rides/{id}, etc.)
  - Response times for each
  - p95, p99 latencies

### Screenshot 3: Database Performance
- **Location:** Click "Databases" tab
- **Capture:**
  - Slow queries
  - Query execution times

### Screenshot 4: Transaction Breakdown
- **Location:** Click on a specific transaction (e.g., "POST /v1/rides")
- **Capture:**
  - Time breakdown (Database, Application, External)
  - Slow query details

---

## ✅ Documentation - YES, MD Files Are Enough!

**Your documentation is complete!** ✅

You have:
- ✅ `README.md` - Setup and usage
- ✅ `ARCHITECTURE.md` - High-Level Design (HLD)
- ✅ `SYSTEM_DESIGN.md` - Low-Level Design (LLD)
- ✅ `API_COMPLETE.md` - API documentation
- ✅ `PERFORMANCE_OPTIMIZATION.md` - Performance report
- ✅ `DELIVERABLES_CHECKLIST.md` - Complete checklist
- ✅ `NEW_RELIC_SETUP_STEPS.md` - New Relic guide
- ✅ `NEW_RELIC_TESTING_GUIDE.md` - Testing guide

**Plus New Relic screenshots** = Complete submission! 🎉

---

## 🐛 Troubleshooting

### No data in dashboard?
1. Check license key is correct
2. Wait 2-3 minutes (data takes time to appear)
3. Refresh the dashboard
4. Check application is running

### Agent not loading?
```bash
# Verify agent exists
ls -la newrelic-agent-8.10.0.jar

# Check logs
ls -la logs/newrelic_agent.log
```

---

## 🎯 Summary

1. ✅ Update license key in `newrelic.yml`
2. ✅ Run: `./gradlew bootRun -Pnewrelic.agent.jar.path=./newrelic-agent-8.10.0.jar`
3. ✅ Generate traffic: `./test-newrelic.sh` or use frontend
4. ✅ Open: https://one.newrelic.com → APM & Services → GoComet DAW
5. ✅ Wait 2-3 minutes
6. ✅ Take screenshots (Overview, Transactions, Databases, Breakdown)
7. ✅ Documentation is complete!

**You're all set!** 🚀

