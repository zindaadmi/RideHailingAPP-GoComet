# New Relic Integration Guide

## Overview

This guide explains how to integrate New Relic monitoring into the GoComet DAW application. New Relic provides comprehensive APM (Application Performance Monitoring) capabilities to track API performance, database queries, and system metrics.

## Prerequisites

1. New Relic account (sign up at https://newrelic.com - 100GB free tier available)
2. New Relic license key
3. Java application running on Java 17+

## Integration Steps

### 1. Add New Relic Java Agent

Add the New Relic Java agent dependency to your `build.gradle`:

```gradle
dependencies {
    // ... existing dependencies
    
    // New Relic Java Agent
    implementation 'com.newrelic.agent.java:newrelic-java:8.10.0'
}
```

### 2. Download New Relic Agent

Alternatively, download the New Relic Java agent JAR file:

```bash
# Download New Relic agent
wget https://download.newrelic.com/newrelic/java-agent/newrelic-agent/8.10.0/newrelic-agent-8.10.0.jar
```

Place it in your project root or a `lib` directory.

### 3. Create New Relic Configuration File

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
    
  # Browser monitoring
  browser_monitoring:
    enabled: false
    
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

development:
  <<: *default_settings
  app_name: GoComet DAW (Dev)

production:
  <<: *default_settings
  app_name: GoComet DAW (Prod)
```

**Important**: Replace `YOUR_LICENSE_KEY_HERE` with your actual New Relic license key.

### 4. Update Application Properties

Add New Relic configuration to `application.properties`:

```properties
# New Relic Configuration
newrelic.config.file=src/main/resources/newrelic.yml
newrelic.environment=development
```

### 5. Run Application with New Relic Agent

#### Option A: Using JVM Arguments

```bash
java -javaagent:newrelic-agent-8.10.0.jar \
     -jar build/libs/GoComet-DAW-0.0.1-SNAPSHOT.jar
```

#### Option B: Using Gradle

Update `build.gradle` to include the agent:

```gradle
bootRun {
    jvmArgs = [
        "-javaagent:${projectDir}/newrelic-agent-8.10.0.jar"
    ]
}
```

### 6. Custom Instrumentation (Optional)

Create custom metrics and transactions:

```java
package com.interview.gocomet.GoComet.DAW.monitoring;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;

public class NewRelicMonitoring {
    
    @Trace(dispatcher = true)
    public void trackDriverMatching(String rideId) {
        NewRelic.setTransactionName("Custom", "DriverMatching");
        NewRelic.addCustomAttribute("rideId", rideId);
        NewRelic.recordMetric("Custom/DriverMatching/Count", 1);
    }
    
    @Trace(dispatcher = true)
    public void trackPaymentProcessing(Long tripId, Double amount) {
        NewRelic.setTransactionName("Custom", "PaymentProcessing");
        NewRelic.addCustomAttribute("tripId", tripId);
        NewRelic.addCustomAttribute("amount", amount);
        NewRelic.recordMetric("Custom/Payment/Amount", amount);
    }
    
    public void recordError(Exception e, String context) {
        NewRelic.noticeError(e);
        NewRelic.addCustomAttribute("errorContext", context);
    }
}
```

### 7. Add Custom Metrics in Services

Example: Add metrics to `DriverMatchingService`:

```java
import com.newrelic.api.agent.NewRelic;

@Service
public class DriverMatchingService {
    
    public Driver matchDriver(Double latitude, Double longitude) {
        long startTime = System.currentTimeMillis();
        
        try {
            // ... matching logic ...
            
            long duration = System.currentTimeMillis() - startTime;
            NewRelic.recordMetric("Custom/DriverMatching/Duration", duration);
            NewRelic.recordMetric("Custom/DriverMatching/Success", 1);
            
            return matchedDriver;
        } catch (Exception e) {
            NewRelic.recordMetric("Custom/DriverMatching/Failure", 1);
            NewRelic.noticeError(e);
            throw e;
        }
    }
}
```

## Metrics to Monitor

### 1. API Performance Metrics

- **Response Time**: Track p50, p95, p99 latencies for each endpoint
- **Throughput**: Requests per second
- **Error Rate**: Percentage of failed requests
- **Apdex Score**: Application performance index

### 2. Database Metrics

- **Query Performance**: Slow query detection
- **Connection Pool**: Active/idle connections
- **Transaction Duration**: Database transaction times
- **Query Count**: Queries per second

### 3. Cache Metrics

- **Cache Hit/Miss Ratio**: Redis cache effectiveness
- **Cache Response Time**: Redis operation latency
- **Cache Size**: Memory usage

### 4. Business Metrics

- **Ride Requests**: Requests per minute
- **Driver Matching**: Success rate and average time
- **Payment Processing**: Success rate and processing time
- **Active Rides**: Concurrent active rides

## Setting Up Alerts

### 1. Create Alert Policies

In New Relic UI:
1. Go to **Alerts & AI** → **Alert Policies**
2. Create a new policy: "GoComet DAW Production"

### 2. Configure Alert Conditions

#### API Latency Alert
- **Metric**: `Apdex`
- **Threshold**: < 0.85
- **Duration**: 5 minutes
- **Action**: Send notification

#### Error Rate Alert
- **Metric**: `Error rate`
- **Threshold**: > 5%
- **Duration**: 5 minutes
- **Action**: Send notification

#### Database Slow Query Alert
- **Metric**: `Database query duration`
- **Threshold**: > 1 second
- **Duration**: 1 minute
- **Action**: Send notification

## Dashboard Configuration

### 1. Create Custom Dashboard

1. Go to **Dashboards** → **Create Dashboard**
2. Name: "GoComet DAW Performance"

### 2. Add Widgets

#### API Performance Widget
- **Query**: `SELECT average(duration) FROM Transaction WHERE appName = 'GoComet DAW'`
- **Chart Type**: Line chart
- **Time Range**: Last 1 hour

#### Database Performance Widget
- **Query**: `SELECT average(duration) FROM Datastore WHERE appName = 'GoComet DAW'`
- **Chart Type**: Bar chart

#### Business Metrics Widget
- **Query**: `SELECT count(*) FROM Transaction WHERE name = 'Custom/DriverMatching'`
- **Chart Type**: Number

## Performance Optimization Based on New Relic Data

### 1. Identify Slow Endpoints

- Review **APM** → **Transactions** to find slow endpoints
- Focus on endpoints with p95 > 500ms
- Optimize database queries or add caching

### 2. Database Query Optimization

- Review **APM** → **Databases** for slow queries
- Add indexes for frequently queried fields
- Optimize N+1 query problems

### 3. Cache Optimization

- Monitor cache hit/miss ratios
- Adjust cache TTLs based on data freshness requirements
- Add caching for frequently accessed data

## Testing the Integration

### 1. Verify Agent is Running

Check application logs for:
```
New Relic Agent: Starting New Relic Agent version X.X.X
```

### 2. Generate Test Traffic

```bash
# Use Apache Bench or similar
ab -n 1000 -c 10 http://localhost:8080/v1/rides
```

### 3. Verify Data in New Relic

1. Go to New Relic UI
2. Navigate to **APM** → **Applications** → **GoComet DAW**
3. Verify transactions are being recorded
4. Check metrics and traces

## Troubleshooting

### Agent Not Starting

- Verify Java agent JAR is in correct location
- Check JVM arguments include `-javaagent`
- Review `newrelic.yml` configuration

### No Data in New Relic

- Verify license key is correct
- Check network connectivity
- Review New Relic agent logs

### High Overhead

- Reduce transaction sampling rate
- Disable unnecessary features (browser monitoring, etc.)
- Optimize custom instrumentation

## Best Practices

1. **Monitor Key Business Metrics**: Track ride requests, matching success, payments
2. **Set Up Alerts Early**: Configure alerts before production deployment
3. **Regular Review**: Review dashboards weekly to identify trends
4. **Performance Baselines**: Establish baseline metrics for comparison
5. **Custom Attributes**: Add business context to transactions (rideId, driverId, etc.)

## Additional Resources

- [New Relic Java Agent Documentation](https://docs.newrelic.com/docs/agents/java-agent/)
- [New Relic API Documentation](https://docs.newrelic.com/docs/apis/)
- [New Relic Best Practices](https://docs.newrelic.com/docs/using-new-relic/welcome-new-relic/getting-started/get-started-new-relic/)

## Next Steps

1. Complete New Relic account setup
2. Add New Relic agent to the application
3. Configure custom metrics for business logic
4. Set up dashboards and alerts
5. Monitor and optimize based on metrics

---

**Note**: This integration should be completed after the backend is fully functional. The frontend can be integrated separately using New Relic Browser monitoring.

