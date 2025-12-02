# How to Open New Relic Dashboard

## Method 1: Direct Link (Easiest)

1. **Go to:** https://one.newrelic.com
2. **Login** with your New Relic account credentials
3. **Click:** "APM & Services" in the left sidebar menu
4. **Find:** "GoComet DAW" in the application list
5. **Click** on "GoComet DAW" to open the dashboard

---

## Method 2: Step-by-Step Navigation

### Step 1: Login
- Open browser
- Go to: **https://one.newrelic.com**
- Enter your email and password
- Click "Sign In"

### Step 2: Navigate to APM
- After login, you'll see the main dashboard
- In the **left sidebar**, look for:
  - **"APM & Services"** (or "Applications")
  - Click on it

### Step 3: Find Your Application
- You'll see a list of applications
- Look for: **"GoComet DAW"**
- If you don't see it, wait 2-3 minutes after starting your app (data needs time to appear)

### Step 4: Open Dashboard
- Click on **"GoComet DAW"**
- You'll see the main dashboard with:
  - Response time graphs
  - Throughput metrics
  - Error rates
  - Apdex score

---

## Method 3: Direct APM Link

After your application sends data for the first time:

**Direct link:** https://one.newrelic.com/apm

Then select "GoComet DAW" from the list.

---

## What You'll See

### Main Dashboard View:
- **Response Time** - Graph showing API response times
- **Throughput** - Requests per minute
- **Error Rate** - Percentage of errors
- **Apdex Score** - Application performance index

### Tabs Available:
1. **Overview** - Main dashboard (default)
2. **Transactions** - Individual API endpoints performance
3. **Databases** - Database query performance
4. **Errors** - Error tracking
5. **JVM** - Java Virtual Machine metrics

---

## Troubleshooting

### Can't find "GoComet DAW"?
1. **Check if app is running** with New Relic agent
2. **Wait 2-3 minutes** - Data takes time to appear
3. **Generate some traffic** - Make API calls or use frontend
4. **Refresh the page** - Press F5 or Cmd+R
5. **Check license key** - Make sure it's correct in `newrelic.yml`

### Not seeing any data?
1. **Verify application is running:**
   ```bash
   # Should see: Started GoCometDawApplication
   ```

2. **Generate traffic:**
   ```bash
   ./test-newrelic.sh
   ```

3. **Wait 2-3 minutes** after first request

4. **Check New Relic logs:**
   ```bash
   ls -la logs/newrelic_agent.log
   ```

### Login issues?
- Make sure you're using the correct account
- Check if you signed up at: https://newrelic.com
- Verify your email is confirmed

---

## Quick Checklist

- ✅ Application running with New Relic agent?
- ✅ License key correct in `newrelic.yml`?
- ✅ Generated some test traffic?
- ✅ Waited 2-3 minutes?
- ✅ Refreshed the dashboard?

---

## Screenshot Locations

Once dashboard is open, take screenshots of:

1. **Main Dashboard** (Overview tab)
   - Response time graph
   - Throughput
   - Error rate

2. **Transactions Tab**
   - List of endpoints
   - Response times

3. **Databases Tab**
   - Query performance

4. **Individual Transaction** (click on an endpoint)
   - Time breakdown

---

**That's it!** The dashboard should be accessible at: **https://one.newrelic.com**

