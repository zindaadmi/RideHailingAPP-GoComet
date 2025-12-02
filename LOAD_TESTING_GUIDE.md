# Load Testing Guide - 15,000+ Requests

## Overview

This guide provides scripts to generate **15,000+ requests** across all APIs for comprehensive load testing and New Relic monitoring.

## Prerequisites

1. ✅ Application running with New Relic agent
2. ✅ Application accessible at `http://localhost:8080`
3. ✅ New Relic dashboard open (to monitor in real-time)

---

## Option 1: Sequential Load Test (Recommended for Monitoring)

**Script:** `load-test.sh`

### Features:
- ✅ 15,000+ requests total
- ✅ Tests all 9 API endpoints
- ✅ Progress indicators
- ✅ Success/failure tracking
- ✅ Detailed summary report

### Usage:

```bash
cd /Users/nikhilparakh/Downloads/GoComet-DAW
./load-test.sh
```

### Request Distribution:
- **POST /v1/rides**: 500 requests
- **GET /v1/rides/{id}**: 3,000 requests
- **POST /v1/drivers/{id}/location**: 3,000 requests
- **POST /v1/drivers/{id}/accept**: 2,000 requests
- **GET /v1/trips/{id}**: 2,000 requests
- **POST /v1/trips/{id}/end**: 1,500 requests
- **POST /v1/payments**: 1,500 requests
- **GET /v1/payments/{id}**: 1,000 requests
- **GET /v1/drivers/{id}**: 1,000 requests

**Total: 15,500 requests**

### Execution Time:
- Approximately **15-20 minutes** (sequential execution)
- Better for monitoring individual request performance

---

## Option 2: Parallel Load Test (Faster)

**Script:** `load-test-parallel.sh`

### Features:
- ✅ 15,000+ requests total
- ✅ Parallel execution (50 concurrent requests)
- ✅ Faster completion (~5-10 minutes)
- ✅ Better for stress testing

### Usage:

```bash
cd /Users/nikhilparakh/Downloads/GoComet-DAW
./load-test-parallel.sh
```

### Execution Time:
- Approximately **5-10 minutes** (parallel execution)
- Better for testing system under concurrent load

---

## Step-by-Step Testing Process

### Step 1: Start Application with New Relic

```bash
cd /Users/nikhilparakh/Downloads/GoComet-DAW
./gradlew bootRun -Pnewrelic.agent.jar.path=./newrelic-agent-8.10.0.jar
```

**Wait for:** `Started GoCometDawApplication`

### Step 2: Open New Relic Dashboard

1. Go to: https://one.newrelic.com
2. Navigate to: **APM & Services** → **GoComet DAW**
3. Keep it open to monitor in real-time

### Step 3: Run Load Test

**Terminal 1 (Application):** Keep running

**Terminal 2 (Load Test):** Run the script

```bash
cd /Users/nikhilparakh/Downloads/GoComet-DAW
./load-test.sh
```

### Step 4: Monitor in Real-Time

Watch the New Relic dashboard as requests come in:
- Response times will update in real-time
- Throughput will increase
- Error rates will be visible
- Database queries will be tracked

### Step 5: Wait for Completion

The script will show progress:
- ✅ = Successful request
- ❌ = Failed request
- Progress every 100 requests

### Step 6: Review Results

After completion, you'll see:
- Total requests
- Success count
- Failure count
- Success rate percentage

---

## What to Monitor in New Relic

### During Load Test:

1. **Response Time Graph**
   - Watch for spikes
   - Check p95, p99 latencies
   - Should stay under 500ms (p95)

2. **Throughput**
   - Requests per minute
   - Should handle 10k+ requests/min

3. **Error Rate**
   - Should stay low (< 1%)
   - Monitor for any errors

4. **Database Performance**
   - Query execution times
   - Slow query identification
   - Connection pool usage

5. **Transaction Breakdown**
   - Click on individual endpoints
   - See time spent in database vs application
   - Identify bottlenecks

### After Load Test:

1. **Take Screenshots:**
   - Overview dashboard with load test data
   - Transactions tab showing all endpoints
   - Database tab with query performance
   - Individual transaction breakdowns

2. **Review Metrics:**
   - Average response time
   - p95, p99 latencies
   - Error rate
   - Throughput achieved

---

## Expected Results

### Performance Targets:
- ✅ **Response Time (p95)**: < 500ms
- ✅ **Throughput**: 10,000+ requests/min
- ✅ **Error Rate**: < 1%
- ✅ **Database Queries**: < 100ms average

### What You Should See:

1. **New Relic Dashboard:**
   - Spike in throughput during test
   - Response time graphs showing load
   - All endpoints visible in Transactions tab

2. **Application Logs:**
   - Request processing logs
   - Cache hits/misses
   - Database query logs

3. **Load Test Output:**
   - Success rate > 95%
   - All phases completed
   - Summary statistics

---

## Troubleshooting

### High Error Rate?
- Check if application is running
- Verify database connection
- Check Redis connection (if using)
- Review application logs

### Slow Response Times?
- Check database indexes
- Verify caching is working
- Check connection pool size
- Review New Relic slow query reports

### Script Not Running?
```bash
# Make sure script is executable
chmod +x load-test.sh

# Check if application is running
curl http://localhost:8080/v1/rides/RIDE-1234567890
```

### No Data in New Relic?
- Wait 2-3 minutes after test starts
- Verify license key is correct
- Check New Relic agent logs: `logs/newrelic_agent.log`
- Refresh dashboard

---

## Customization

### Adjust Request Counts:

Edit `load-test.sh` and change the numbers:
```bash
# Example: Increase GET requests to 5,000
for i in $(seq 1 5000); do
    # ...
done
```

### Adjust Concurrent Requests:

Edit `load-test-parallel.sh`:
```bash
CONCURRENT_REQUESTS=100  # Increase for more load
```

### Test Specific Endpoint:

Create a focused test:
```bash
# Test only ride creation
for i in $(seq 1 5000); do
    curl -X POST http://localhost:8080/v1/rides \
        -H "Content-Type: application/json" \
        -d '{"riderId":"RIDER-'$i'",...}'
done
```

---

## Summary

✅ **Scripts Ready:**
- `load-test.sh` - Sequential (15-20 min)
- `load-test-parallel.sh` - Parallel (5-10 min)

✅ **Total Requests:** 15,500+

✅ **All APIs Tested:** 9 endpoints

✅ **Monitoring:** New Relic dashboard

**Run the test and watch your New Relic dashboard come alive with data!** 🚀

