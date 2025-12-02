# GoComet DAW - System Design Document

## Table of Contents
1. [Executive Summary](#executive-summary)
2. [System Overview](#system-overview)
3. [Architecture Design](#architecture-design)
4. [Component Design](#component-design)
5. [Database Design](#database-design)
6. [API Design](#api-design)
7. [Scalability Design](#scalability-design)
8. [Performance Optimization](#performance-optimization)
9. [Security Design](#security-design)
10. [Monitoring & Observability](#monitoring--observability)
11. [Deployment Architecture](#deployment-architecture)
12. [Failure Handling](#failure-handling)
13. [Future Enhancements](#future-enhancements)

---

## Executive Summary

GoComet DAW is a multi-tenant, multi-region ride-hailing platform designed to handle:
- **100,000 concurrent drivers**
- **10,000 ride requests per minute**
- **200,000 location updates per second**
- **Sub-second driver matching** (p95 < 1 second)

The system is built using microservices architecture principles with Spring Boot, PostgreSQL, and Redis, ensuring high availability, scalability, and low latency.

---

## System Overview

### Business Requirements

1. **Real-time Driver Location Updates**: Drivers update location 1-2 times per second
2. **Ride Request Processing**: Handle ride requests with pickup/destination, tier, and payment method
3. **Driver-Rider Matching**: Match available drivers to ride requests within 1 second (p95)
4. **Trip Lifecycle Management**: Complete trip management (start, pause, resume, end)
5. **Fare Calculation**: Dynamic fare calculation based on distance, time, and surge
6. **Payment Processing**: Integration with external Payment Service Providers (PSPs)
7. **Real-time Notifications**: Notify users about key ride events

### Non-Functional Requirements

- **Availability**: 99.9% uptime
- **Scalability**: Horizontal scaling capability
- **Performance**: 
  - API response time: p95 < 500ms
  - Driver matching: p95 < 1s
  - Location update processing: < 100ms
- **Consistency**: Strong consistency for critical operations
- **Reliability**: Handle failures gracefully with retry mechanisms

---

## Architecture Design

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                         Client Layer                            │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐        │
│  │ Web Browser  │  │ Mobile App   │  │ Admin Panel  │        │
│  └──────────────┘  └──────────────┘  └──────────────┘        │
└───────────────────────────┬─────────────────────────────────────┘
                            │
                            │ HTTPS/REST API
                            │
┌───────────────────────────▼─────────────────────────────────────┐
│                      API Gateway Layer                            │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐        │
│  │ Load         │  │ Rate         │  │ Auth          │        │
│  │ Balancer     │  │ Limiter      │  │ Gateway       │        │
│  └──────────────┘  └──────────────┘  └──────────────┘        │
└───────────────────────────┬─────────────────────────────────────┘
                            │
┌───────────────────────────▼─────────────────────────────────────┐
│                  Application Service Layer                        │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐        │
│  │ Ride        │  │ Driver       │  │ Payment      │        │
│  │ Service     │  │ Matching     │  │ Service      │        │
│  │             │  │ Service     │  │              │        │
│  └──────────────┘  └──────────────┘  └──────────────┘        │
│  ┌──────────────┐  ┌──────────────┐                           │
│  │ Trip         │  │ Driver       │                           │
│  │ Service     │  │ Service     │                           │
│  └──────────────┘  └──────────────┘                           │
└───────────────────────────┬─────────────────────────────────────┘
                            │
┌───────────────────────────▼─────────────────────────────────────┐
│                        Data Layer                                 │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐        │
│  │ PostgreSQL   │  │ Redis Cache  │  │ (Future:      │        │
│  │ (Primary DB) │  │ (Hot Data)   │  │  Message     │        │
│  │              │  │              │  │  Queue)      │        │
│  └──────────────┘  └──────────────┘  └──────────────┘        │
└───────────────────────────┬─────────────────────────────────────┘
                            │
┌───────────────────────────▼─────────────────────────────────────┐
│                    External Services                               │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐        │
│  │ Payment      │  │ New Relic    │  │ Notification │        │
│  │ Gateway      │  │ (Monitoring) │  │ Service      │        │
│  │ (PSP)        │  │              │  │              │        │
│  └──────────────┘  └──────────────┘  └──────────────┘        │
└─────────────────────────────────────────────────────────────────┘
```

### Architecture Principles

1. **Stateless Services**: All services are stateless, enabling horizontal scaling
2. **Region-Local Writes**: Writes are kept within the same region to minimize latency
3. **Caching Strategy**: Multi-layer caching (Redis for hot data, application-level caching)
4. **Database Indexing**: Optimized indexes for spatial and status queries
5. **Idempotency**: All write operations support idempotency keys
6. **Transaction Management**: ACID transactions for critical operations
7. **Async Processing**: Non-critical operations processed asynchronously

---

## Component Design

### 1. Ride Service

**Responsibilities:**
- Create ride requests with idempotency
- Match drivers to rides
- Manage ride lifecycle state transitions
- Cache ride status for fast lookups

**State Machine:**
```
PENDING → MATCHED → ACCEPTED → IN_PROGRESS → COMPLETED
         ↓          ↓          ↓
      CANCELLED  CANCELLED  CANCELLED
```

**Key Methods:**
- `createRide(RideRequest)`: Creates ride and attempts driver matching
- `getRide(String rideId)`: Retrieves ride status (cached)
- `updateRideStatus(String, RideStatus)`: Updates ride state
- `linkTrip(String, Long)`: Links trip to ride

**Caching Strategy:**
- Cache key: `rides:{rideId}`
- TTL: 5 minutes
- Invalidation: On status change

### 2. Driver Matching Service

**Responsibilities:**
- Find available drivers within radius
- Match closest driver to ride request
- Handle concurrent matching requests (optimistic locking)
- Cache available driver lists by location

**Matching Algorithm:**
1. Calculate bounding box (10km radius from pickup)
2. Query database for available drivers in bounding box
3. Calculate Haversine distance for each driver
4. Sort by distance
5. Attempt to assign closest available driver (with optimistic locking)
6. Fallback to next driver if assignment fails

**Performance Optimizations:**
- Spatial indexing on (latitude, longitude)
- Composite index on (status, latitude, longitude)
- Redis caching of available driver lists
- Batch processing for multiple requests

**Concurrency Handling:**
- Optimistic locking using version fields
- Database-level locking for critical sections
- Retry mechanism for failed assignments

### 3. Driver Service

**Responsibilities:**
- Update driver location (1-2 updates per second)
- Accept ride assignments
- Release drivers after trip completion
- Cache driver locations in Redis

**Location Update Flow:**
1. Receive location update
2. Validate coordinates
3. Update database
4. Cache location in Redis (TTL: 5 seconds)
5. Invalidate available drivers cache

**Performance Considerations:**
- Batch location updates when possible
- Async processing for non-critical updates
- Connection pooling for database

### 4. Trip Service

**Responsibilities:**
- Start trips
- End trips and calculate fare
- Handle trip pausing/resuming
- Calculate distance and duration

**Fare Calculation Formula:**
```
Base Fare: ₹25
Distance Fare: ₹8/km
Time Fare: ₹1/minute
Surge Multiplier: 1.0 - 3.0 (dynamic based on demand)
Total = (Base + Distance + Time) × Surge
Minimum Fare: ₹40
```

**Distance Calculation:**
- Uses Haversine formula for great-circle distance
- Accounts for pause duration in time calculation
- Stores calculated values for audit trail

### 5. Payment Service

**Responsibilities:**
- Process payments via external PSP
- Handle payment retries
- Maintain payment status
- Support idempotency

**Payment Flow:**
1. Validate trip is completed
2. Create payment record
3. Call external PSP (async)
4. Update payment status
5. Handle failures with retry logic

**Retry Strategy:**
- Exponential backoff
- Maximum 3 retries
- Dead letter queue for failed payments

---

## Database Design

### Entity Relationship Diagram

```
┌─────────────┐         ┌─────────────┐         ┌─────────────┐
│   Driver    │         │    Ride     │         │    Trip     │
├─────────────┤         ├─────────────┤         ├─────────────┤
│ id (PK)     │         │ id (PK)     │         │ id (PK)     │
│ driver_id   │◄────────┤ driver_id   │◄────────┤ ride_id     │
│ status      │         │ ride_id     │         │ driver_id   │
│ latitude    │         │ rider_id    │         │ status      │
│ longitude   │         │ status      │         │ total_fare   │
│ ...         │         │ ...         │         │ ...         │
└─────────────┘         └─────────────┘         └─────────────┘
                                                       │
                                                       │
                                                       ▼
                                                ┌─────────────┐
                                                │  Payment    │
                                                ├─────────────┤
                                                │ id (PK)     │
                                                │ trip_id     │
                                                │ amount      │
                                                │ status      │
                                                │ ...         │
                                                └─────────────┘
```

### Database Schema Details

#### Drivers Table
- **Primary Key**: `id`
- **Unique Index**: `driver_id`
- **Indexes**: 
  - `idx_driver_status` on `status`
  - `idx_driver_location` on `(latitude, longitude)`
  - `idx_driver_available` on `(status, latitude, longitude)`

#### Rides Table
- **Primary Key**: `id`
- **Unique Index**: `ride_id`, `idempotency_key`
- **Indexes**:
  - `idx_ride_status` on `status`
  - `idx_ride_rider` on `rider_id`
  - `idx_ride_driver` on `driver_id`
  - `idx_ride_created` on `created_at`

#### Trips Table
- **Primary Key**: `id`
- **Unique Index**: `trip_id`
- **Indexes**:
  - `idx_trip_ride` on `ride_id`
  - `idx_trip_status` on `status`
  - `idx_trip_driver` on `driver_id`

#### Payments Table
- **Primary Key**: `id`
- **Unique Index**: `payment_id`
- **Indexes**:
  - `idx_payment_trip` on `trip_id`
  - `idx_payment_status` on `status`
  - `idx_payment_rider` on `rider_id`

### Database Optimization Strategies

1. **Indexing Strategy**
   - Composite indexes for multi-column queries
   - Partial indexes for filtered queries
   - Covering indexes to avoid table lookups

2. **Partitioning** (Future)
   - Partition by date for historical data
   - Partition by region for multi-region deployment

3. **Connection Pooling**
   - HikariCP with optimal pool size
   - Read/write splitting for read-heavy operations

4. **Query Optimization**
   - Use EXPLAIN ANALYZE for slow queries
   - Avoid N+1 query problems
   - Use batch operations for bulk updates

---

## API Design

### RESTful API Principles

1. **Resource-Based URLs**: `/v1/rides`, `/v1/drivers`, `/v1/trips`
2. **HTTP Methods**: GET (read), POST (create), PUT (update), DELETE (delete)
3. **Status Codes**: Proper HTTP status codes
4. **Error Handling**: Consistent error response format
5. **Versioning**: URL-based versioning (`/v1/`)

### API Endpoints

#### Ride Management

**POST /v1/rides**
- Creates a new ride request
- Returns ride details with status
- Supports idempotency via `idempotencyKey`

**GET /v1/rides/{id}**
- Retrieves ride status
- Cached response for performance
- Returns 404 if not found

#### Driver Management

**POST /v1/drivers/{id}/location**
- Updates driver location
- High-frequency endpoint (1-2 updates/sec)
- Optimized for low latency

**POST /v1/drivers/{id}/accept**
- Driver accepts ride assignment
- Updates ride and driver status
- Transactional operation

#### Trip Management

**POST /v1/trips/{id}/end**
- Ends trip and calculates fare
- Updates trip status to COMPLETED
- Releases driver for next ride

#### Payment

**POST /v1/payments**
- Processes payment for completed trip
- Integrates with external PSP
- Supports idempotency

### API Response Formats

**Success Response:**
```json
{
  "rideId": "RIDE-123",
  "status": "MATCHED",
  "driverId": 1,
  ...
}
```

**Error Response:**
```json
{
  "error": "Error message description"
}
```

### Rate Limiting

- **Location Updates**: 10 requests/second per driver
- **Ride Requests**: 100 requests/minute per rider
- **General APIs**: 1000 requests/minute per IP

---

## Scalability Design

### Horizontal Scaling

1. **Stateless Services**: All services are stateless, enabling easy horizontal scaling
2. **Load Balancing**: Multiple instances behind load balancer
3. **Database Scaling**: Read replicas for read-heavy operations
4. **Cache Scaling**: Redis cluster for distributed caching

### Vertical Scaling

1. **Database**: Optimize queries, add indexes, increase resources
2. **Application**: Increase JVM heap size, optimize garbage collection
3. **Cache**: Increase Redis memory, optimize eviction policies

### Scaling Strategies by Component

#### Driver Matching Service
- **Current**: Single instance
- **Scale To**: Multiple instances with shared Redis cache
- **Bottleneck**: Database queries
- **Solution**: Read replicas, better indexing

#### Location Update Service
- **Current**: Single instance
- **Scale To**: Multiple instances with message queue
- **Bottleneck**: Database writes
- **Solution**: Batch writes, async processing

#### Ride Service
- **Current**: Single instance
- **Scale To**: Multiple instances
- **Bottleneck**: Database queries
- **Solution**: Caching, read replicas

### Capacity Planning

**Current Capacity:**
- 100k drivers
- 10k ride requests/minute
- 200k location updates/second

**Scaling Targets:**
- 1M drivers (10x)
- 100k ride requests/minute (10x)
- 2M location updates/second (10x)

**Scaling Approach:**
1. Add more application instances
2. Scale database (read replicas, sharding)
3. Scale Redis (cluster mode)
4. Add message queue for async processing

---

## Performance Optimization

### Database Optimization

1. **Indexing**
   - Composite indexes for multi-column queries
   - Partial indexes for filtered queries
   - Covering indexes to avoid table lookups

2. **Query Optimization**
   - Use EXPLAIN ANALYZE
   - Avoid N+1 queries
   - Use batch operations
   - Optimize JOINs

3. **Connection Pooling**
   - HikariCP with optimal pool size
   - Monitor connection pool metrics

### Caching Strategy

1. **Redis Caching**
   - Hot data: Ride status, driver locations
   - TTL: 5 minutes for rides, 5 seconds for locations
   - Cache invalidation on writes

2. **Application-Level Caching**
   - Cache frequently accessed data
   - Use Spring Cache abstraction

3. **Cache Patterns**
   - Cache-Aside pattern
   - Write-Through for critical data
   - Write-Behind for non-critical data

### Code Optimization

1. **Async Processing**
   - Non-critical operations processed asynchronously
   - Use CompletableFuture for async operations

2. **Batch Processing**
   - Batch database operations
   - Batch cache operations

3. **Connection Reuse**
   - Reuse HTTP connections
   - Connection pooling

### Performance Targets

- **API Response Time**: p95 < 500ms
- **Driver Matching**: p95 < 1s
- **Location Update**: p95 < 100ms
- **Database Queries**: p95 < 200ms
- **Cache Operations**: p95 < 10ms

---

## Security Design

### Authentication & Authorization

1. **API Keys**: For service-to-service communication
2. **JWT Tokens**: For user authentication (future)
3. **OAuth 2.0**: For third-party integrations (future)

### Data Security

1. **Encryption**
   - TLS/SSL for data in transit
   - Encryption at rest for sensitive data
   - Encrypt payment information

2. **Input Validation**
   - Validate all inputs
   - Sanitize user inputs
   - Prevent SQL injection

3. **Access Control**
   - Role-based access control (RBAC)
   - Principle of least privilege

### Security Best Practices

1. **Rate Limiting**: Prevent abuse
2. **CORS Configuration**: Restrict origins
3. **Error Handling**: Don't expose sensitive information
4. **Logging**: Log security events
5. **Audit Trail**: Track all operations

---

## Monitoring & Observability

### Metrics to Monitor

1. **Application Metrics**
   - Request rate
   - Response time (p50, p95, p99)
   - Error rate
   - Apdex score

2. **Business Metrics**
   - Ride requests per minute
   - Driver matching success rate
   - Average matching time
   - Payment success rate

3. **Infrastructure Metrics**
   - CPU usage
   - Memory usage
   - Database connections
   - Cache hit/miss ratio

### Logging Strategy

1. **Structured Logging**
   - JSON format
   - Correlation IDs
   - Log levels (INFO, WARN, ERROR)

2. **Log Aggregation**
   - Centralized logging system
   - Log retention policy
   - Log analysis tools

### Alerting

1. **Alert Conditions**
   - High error rate (> 5%)
   - Slow response time (p95 > 1s)
   - Database connection pool exhaustion
   - Cache hit rate < 80%

2. **Alert Channels**
   - Email
   - SMS
   - Slack/PagerDuty

### New Relic Integration

- APM for application performance
- Infrastructure monitoring
- Custom dashboards
- Alert policies

---

## Deployment Architecture

### Environment Setup

1. **Development**
   - Single instance
   - H2 database
   - Local Redis

2. **Staging**
   - Multiple instances
   - PostgreSQL
   - Redis cluster

3. **Production**
   - Multiple instances across regions
   - PostgreSQL with read replicas
   - Redis cluster
   - Load balancers

### Deployment Strategy

1. **Blue-Green Deployment**
   - Zero-downtime deployments
   - Quick rollback capability

2. **Canary Deployment**
   - Gradual rollout
   - Monitor metrics before full deployment

### Infrastructure Components

1. **Application Servers**
   - Spring Boot applications
   - Docker containers
   - Kubernetes orchestration (future)

2. **Database**
   - PostgreSQL primary
   - Read replicas
   - Automated backups

3. **Cache**
   - Redis cluster
   - High availability
   - Persistence enabled

4. **Load Balancers**
   - Application load balancer
   - Health checks
   - SSL termination

---

## Failure Handling

### Failure Scenarios

1. **Database Failure**
   - Read from replicas
   - Graceful degradation
   - Retry with exponential backoff

2. **Cache Failure**
   - Fallback to database
   - Degraded performance
   - Cache warming on recovery

3. **External Service Failure**
   - Circuit breaker pattern
   - Retry with backoff
   - Fallback mechanisms

### Resilience Patterns

1. **Circuit Breaker**
   - Prevent cascade failures
   - Automatic recovery
   - Fallback responses

2. **Retry Logic**
   - Exponential backoff
   - Maximum retry attempts
   - Idempotent operations

3. **Timeout Handling**
   - Request timeouts
   - Connection timeouts
   - Graceful timeout responses

---

## Future Enhancements

### Short-Term (3-6 months)

1. **Message Queue Integration**
   - Kafka/RabbitMQ for async processing
   - Event-driven architecture
   - Better scalability

2. **WebSocket Support**
   - Real-time updates
   - Push notifications
   - Live tracking

3. **Surge Pricing**
   - Dynamic pricing based on demand
   - ML-based price optimization
   - Real-time price updates

### Medium-Term (6-12 months)

1. **Multi-Region Deployment**
   - Cross-region replication
   - Regional failover
   - Data locality

2. **Advanced Matching**
   - ML-based driver-rider matching
   - Preference-based matching
   - Historical data analysis

3. **Analytics Platform**
   - Real-time dashboards
   - Business intelligence
   - Predictive analytics

### Long-Term (12+ months)

1. **Microservices Architecture**
   - Service decomposition
   - Independent scaling
   - Service mesh

2. **Event Sourcing**
   - Complete audit trail
   - Time travel debugging
   - Event replay

3. **GraphQL API**
   - Flexible queries
   - Reduced over-fetching
   - Better mobile support

---

## Conclusion

This system design provides a scalable, performant, and reliable foundation for the GoComet DAW ride-hailing platform. The architecture is designed to handle high traffic with sub-second response times while maintaining data consistency and reliability.

Key strengths:
- **Scalability**: Horizontal scaling capability
- **Performance**: Optimized for low latency
- **Reliability**: Fault-tolerant design
- **Maintainability**: Clean architecture and documentation

The system is ready for production deployment with proper monitoring, alerting, and operational procedures in place.

