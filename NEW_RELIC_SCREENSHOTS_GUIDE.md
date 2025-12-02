# New Relic Screenshots Guide - What to Capture

## Required Screenshots for Submission

Take these **5 essential screenshots** from your New Relic dashboard:

---

## Screenshot 1: Overview Dashboard ⭐ (MOST IMPORTANT)

**Location:** Main application page (first page when you click "GoComet DAW")

**What to Capture:**
- ✅ Response time graph (showing load test spike)
- ✅ Throughput graph (requests per minute)
- ✅ Error rate percentage
- ✅ Apdex score
- ✅ Top transactions list

**How to get there:**
1. Go to: https://one.eu.newrelic.com
2. Click: **APM & Services**
3. Click: **GoComet DAW**
4. You're on the Overview page - take screenshot

**Why it's important:** Shows overall application health and performance during load test

---

## Screenshot 2: Transactions Tab (API Performance)

**Location:** Click "Transactions" tab at the top

**What to Capture:**
- ✅ List of all API endpoints:
  - POST /v1/rides
  - GET /v1/rides/{id}
  - POST /v1/drivers/{id}/location
  - POST /v1/drivers/{id}/accept
  - GET /v1/trips/{id}
  - POST /v1/trips/{id}/end
  - POST /v1/payments
  - GET /v1/payments/{id}
  - GET /v1/drivers/{id}
- ✅ Response times for each endpoint
- ✅ p95, p99 latencies
- ✅ Throughput (requests/min) for each

**How to get there:**
1. From Overview page
2. Click: **"Transactions"** tab (top navigation)
3. Take screenshot of the transactions table

**Why it's important:** Shows performance of each API endpoint individually

---

## Screenshot 3: Databases Tab (Query Performance)

**Location:** Click "Databases" tab

**What to Capture:**
- ✅ List of database queries
- ✅ Query execution times
- ✅ Slow queries (if any)
- ✅ Database response times
- ✅ Most time-consuming queries

**How to get there:**
1. From Overview page
2. Click: **"Databases"** tab (top navigation)
3. Take screenshot

**Why it's important:** Shows database performance and query optimization results

---

## Screenshot 4: Individual Transaction Breakdown

**Location:** Click on a specific transaction (e.g., "POST /v1/rides")

**What to Capture:**
- ✅ Transaction name (e.g., "POST /v1/rides")
- ✅ Time breakdown:
  - Database time
  - Application time
  - External time
- ✅ Slow query details (if any)
- ✅ Transaction trace information

**How to get there:**
1. Go to **Transactions** tab
2. Click on **"POST /v1/rides"** (or any endpoint)
3. Take screenshot of the breakdown page

**Why it's important:** Shows where time is spent (database vs application logic)

---

## Screenshot 5: Errors Tab (If Errors Exist)

**Location:** Click "Errors" tab

**What to Capture:**
- ✅ Error rate percentage
- ✅ Error types (if any)
- ✅ Error frequency
- ✅ Error details

**How to get there:**
1. From Overview page
2. Click: **"Errors"** tab (top navigation)
3. Take screenshot

**Why it's important:** Shows error handling and system reliability

**Note:** If no errors, you can skip this or show "No errors" message

---

## Bonus Screenshot: Performance Summary

**Location:** Overview page - scroll down

**What to Capture:**
- ✅ Response time summary (average, p95, p99)
- ✅ Throughput summary
- ✅ Apdex score details
- ✅ Key metrics table

**Why it's important:** Provides summary statistics for the report

---

## Quick Checklist

Before taking screenshots, make sure:

- [ ] Load test has completed (15,500 requests)
- [ ] Wait 2-3 minutes after load test for data to appear
- [ ] Refresh the New Relic dashboard
- [ ] All tabs are visible and loaded

---

## Screenshot Tips

1. **Full Screen:** Capture the entire browser window or use full-page screenshot
2. **High Resolution:** Make sure text is readable
3. **Include Timestamps:** New Relic shows time ranges - make sure they're visible
4. **Multiple Views:** If data is long, take multiple screenshots or scroll to show key metrics
5. **Date/Time Visible:** Ensure the time range selector is visible

---

## What Each Screenshot Proves

| Screenshot | What It Shows |
|------------|---------------|
| **Overview** | Overall system performance, throughput, response times |
| **Transactions** | Individual API endpoint performance |
| **Databases** | Query optimization and database performance |
| **Transaction Breakdown** | Performance bottlenecks (DB vs App) |
| **Errors** | System reliability and error handling |

---

## Submission Format

**Save screenshots as:**
- `newrelic-overview.png`
- `newrelic-transactions.png`
- `newrelic-databases.png`
- `newrelic-transaction-breakdown.png`
- `newrelic-errors.png` (optional)

**Or create a PDF:** Combine all screenshots into one PDF document

---

## Summary

**Minimum Required:**
1. ✅ Overview Dashboard
2. ✅ Transactions Tab
3. ✅ Databases Tab
4. ✅ Transaction Breakdown

**Optional but Recommended:**
5. Errors Tab (if errors exist)
6. Performance Summary

**Total: 4-6 screenshots**

These screenshots, along with your `PERFORMANCE_OPTIMIZATION.md` document, will provide a complete performance report for your submission! 📊

