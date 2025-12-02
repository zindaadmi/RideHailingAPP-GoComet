# New Relic Integration - Step by Step Guide

## Quick Steps (5-10 minutes)

### Step 1: Sign Up for New Relic Account
1. Go to https://newrelic.com
2. Click "Sign Up" (100GB free tier available)
3. Complete registration
4. **Copy your License Key** (you'll find it in Account Settings → API Keys)

### Step 2: Download New Relic Agent
```bash
cd /Users/nikhilparakh/Downloads/GoComet-DAW

# Download the agent JAR
wget https://download.newrelic.com/newrelic/java-agent/newrelic-agent/8.10.0/newrelic-agent-8.10.0.jar

# Or use curl
curl -O https://download.newrelic.com/newrelic/java-agent/newrelic-agent/8.10.0/newrelic-agent-8.10.0.jar
```

### Step 3: Create Configuration File
Create `newrelic.yml` in `src/main/resources/`:

```yaml
common: &default_settings
  app_name: GoComet DAW
  license_key: YOUR_LICENSE_KEY_HERE
  
  # Logging
  log_file_path: logs
  log_level: info
  
  # Transaction tracing
  transaction_tracer:
    enabled: true
    record_sql: obfuscated
    stack_trace_threshold: 0.5
    
  # Error collection
  error_collector:
    enabled: true
    capture_events: true
    
  # Thread profiler
  thread_profiler:
    enabled: true
    
  # Distributed tracing
  distributed_tracing:
    enabled: true
    
  # Application logging
  application_logging:
    enabled: true
    forwarding:
      enabled: true
    metrics:
      enabled: true
    local_decorating:
      enabled: true
```

**⚠️ IMPORTANT**: Replace `YOUR_LICENSE_KEY_HERE` with your actual license key from Step 1.

### Step 4: Update build.gradle (Optional - if using Maven dependency)
If you prefer using Maven dependency instead of JAR file, add to `build.gradle`:

```gradle
dependencies {
    // ... existing dependencies
    implementation 'com.newrelic.agent.java:newrelic-java:8.10.0'
}
```

**Note**: Using the JAR file (Step 2) is simpler and recommended.

### Step 5: Run Application with New Relic Agent

#### Option A: Using Gradle (Recommended)
```bash
./gradlew bootRun -Pnewrelic.agent.jar.path=./newrelic-agent-8.10.0.jar
```

#### Option B: Using JVM Arguments
```bash
java -javaagent:newrelic-agent-8.10.0.jar \
     -jar build/libs/GoComet-DAW-0.0.1-SNAPSHOT.jar
```

#### Option C: Update build.gradle for automatic agent loading
Add to `build.gradle`:

```gradle
bootRun {
    if (project.hasProperty('newrelic.agent.jar.path')) {
        jvmArgs = [
            "-javaagent:${project.property('newrelic.agent.jar.path')}"
        ]
    }
}
```

Then run:
```bash
./gradlew bootRun -Pnewrelic.agent.jar.path=./newrelic-agent-8.10.0.jar
```

### Step 6: Verify Integration
1. Start your application
2. Generate some traffic (use the dashboard or API)
3. Wait 2-3 minutes
4. Go to New Relic dashboard: https://one.newrelic.com
5. Navigate to **APM** → **Applications** → **GoComet DAW**
6. You should see metrics and transactions

## Verification Checklist

- [ ] New Relic account created
- [ ] License key copied
- [ ] Agent JAR downloaded
- [ ] `newrelic.yml` created with license key
- [ ] Application started with `-javaagent` flag
- [ ] Data visible in New Relic dashboard

## Troubleshooting

### Agent Not Starting
- Check if JAR file exists: `ls -la newrelic-agent-8.10.0.jar`
- Verify license key in `newrelic.yml`
- Check application logs for New Relic messages

### No Data in Dashboard
- Wait 2-3 minutes for data to appear
- Verify license key is correct
- Check network connectivity
- Look for errors in `logs/newrelic.log`

### High Overhead
- Reduce transaction sampling rate in `newrelic.yml`
- Disable browser monitoring (already disabled)
- Optimize custom instrumentation

## What You'll See in New Relic

1. **APM Dashboard**: API response times, throughput, error rates
2. **Database Performance**: Query execution times, slow queries
3. **Transaction Traces**: Detailed request/response traces
4. **Error Analytics**: Errors and exceptions
5. **Custom Metrics**: Business metrics (if added)

## Next Steps

1. Set up alerts for slow response times
2. Create custom dashboards
3. Add custom metrics for business logic
4. Monitor database query performance

## Quick Reference

**New Relic Dashboard**: https://one.newrelic.com  
**License Key Location**: Account Settings → API Keys  
**Agent Download**: https://download.newrelic.com/newrelic/java-agent/newrelic-agent/8.10.0/newrelic-agent-8.10.0.jar  
**Documentation**: See `NEW_RELIC_INTEGRATION.md` for detailed guide

