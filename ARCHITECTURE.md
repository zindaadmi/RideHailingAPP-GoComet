# GoComet DAW - Ride Hailing Application Architecture

## Table of Contents
1. [High Level Design (HLD)](#high-level-design-hld)
2. [Low Level Design (LLD)](#low-level-design-lld)
3. [System Components](#system-components)
4. [Database Schema](#database-schema)
5. [API Design](#api-design)
6. [Scalability & Performance](#scalability--performance)
7. [Security](#security)
8. [Monitoring & Observability](#monitoring--observability)

---

## High Level Design (HLD)

### System Overview

The GoComet DAW is a multi-tenant, multi-region ride-hailing platform designed to handle:
- **100k drivers** concurrently
- **10k ride requests per minute**
- **200k location updates per second**
- **Sub-second driver matching** (p95 < 1s)

### Architecture Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                        Client Layer                         │
│  (Web Frontend / Mobile Apps / Admin Dashboard)            │
└────────────────────┬────────────────────────────────────────┘
                     │
                     │ HTTPS/REST API
                     │
┌────────────────────▼────────────────────────────────────────┐
│                    API Gateway Layer                        │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │
│  │ Ride API     │  │ Driver API   │  │ Payment API  │     │
│  └──────────────┘  └──────────────┘  └──────────────┘     │
└────────────────────┬────────────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────────────┐
│                  Application Service Layer                   │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │
│  │ Ride Service │  │ Driver       │  │ Payment      │     │
│  │              │  │ Matching     │  │ Service      │     │
│  │              │  │ Service      │  │              │     │
│  └──────────────┘  └──────────────┘  └──────────────┘     │
│  ┌──────────────┐  ┌──────────────┐                       │
│  │ Trip Service │  │ Driver       │                       │
│  │              │  │ Service      │                       │
│  └──────────────┘  └──────────────┘                       │
└────────────────────┬────────────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────────────┐
│                    Data Layer                                │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │
│  │ PostgreSQL   │  │ Redis Cache  │  │ (Future:     │     │
│  │ (Primary DB) │  │ (Hot Data)   │  │  Message     │     │
│  │              │  │              │  │  Queue)      │     │
│  └──────────────┘  └──────────────┘  └──────────────┘     │
└─────────────────────────────────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────────────┐
│                  External Services                           │
│  ┌──────────────┐  ┌──────────────┐                        │
│  │ Payment      │  │ New Relic   │                        │
│  │ Gateway      │  │ (Monitoring)│                        │
│  │ (PSP)        │  │              │                        │
│  └──────────────┘  └──────────────┘                        │
└─────────────────────────────────────────────────────────────┘
```

### Key Design Principles

1. **Stateless Services**: All services are stateless, enabling horizontal scaling
2. **Region-Local Writes**: Writes are kept within the same region to minimize latency
3. **Caching Strategy**: Redis for hot data (driver locations, ride status)
4. **Database Indexing**: Optimized indexes for spatial and status queries
5. **Idempotency**: All write operations support idempotency keys
6. **Transaction Management**: ACID transactions for critical operations

---

## Low Level Design (LLD)

### Component Details

#### 1. Ride Service

**Responsibilities:**
- Create ride requests with idempotency
- Match drivers to rides
- Manage ride lifecycle (PENDING → MATCHED → ACCEPTED → IN_PROGRESS → COMPLETED)
- Cache ride status for fast lookups

**Key Methods:**
- `createRide(RideRequest)`: Creates ride and attempts driver matching
- `getRide(String rideId)`: Retrieves ride status (cached)
- `updateRideStatus(String, RideStatus)`: Updates ride state

**Caching Strategy:**
- Cache key: `rides:{rideId}`
- TTL: 5 minutes
- Invalidation: On status change

#### 2. Driver Matching Service

**Responsibilities:**
- Find available drivers within radius
- Match closest driver to ride request
- Handle concurrent matching requests (optimistic locking)
- Cache available driver lists by location

**Algorithm:**
1. Calculate bounding box (10km radius)
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

#### 3. Driver Service

**Responsibilities:**
- Update driver location (1-2 updates per second)
- Accept ride assignments
- Release drivers after trip completion
- Cache driver locations in Redis

**Location Update Flow:**
1. Receive location update
2. Update database
3. Cache location in Redis (TTL: 5 seconds)
4. Invalidate available drivers cache

#### 4. Trip Service

**Responsibilities:**
- Start trips
- End trips and calculate fare
- Handle trip pausing/resuming
- Calculate distance and duration

**Fare Calculation:**
```
Base Fare: ₹25
Distance Fare: ₹8/km
Time Fare: ₹1/minute
Surge Multiplier: 1.0 - 3.0 (dynamic)
Total = (Base + Distance + Time) × Surge
Minimum Fare: ₹40
```

#### 5. Payment Service

**Responsibilities:**
- Process payments via external PSP
- Handle payment retries
- Maintain payment status
- Support idempotency

**Payment Flow:**
1. Validate trip is completed
2. Create payment record
3. Call external PSP
4. Update payment status
5. Handle failures with retry logic

---

## System Components

### Technology Stack

- **Framework**: Spring Boot 3.2.0
- **Language**: Java 17
- **Database**: PostgreSQL (primary), H2 (testing)
- **Cache**: Redis
- **Build Tool**: Gradle
- **API**: RESTful APIs
- **Validation**: Jakarta Validation
- **ORM**: Spring Data JPA / Hibernate

### Package Structure

```
com.interview.gocomet.GoComet.DAW
├── controller/          # REST Controllers
├── service/             # Business Logic
├── repository/          # Data Access Layer
├── model/               # Domain Entities
├── dto/                 # Data Transfer Objects
├── config/              # Configuration Classes
└── exception/           # Exception Handlers
```

---

## Database Schema

### Tables

#### 1. drivers
```sql
CREATE TABLE drivers (
    id BIGSERIAL PRIMARY KEY,
    driver_id VARCHAR(255) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    phone_number VARCHAR(20) NOT NULL,
    vehicle_number VARCHAR(50) NOT NULL,
    vehicle_type VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    last_location_update TIMESTAMP NOT NULL,
    current_ride_id BIGINT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_driver_status ON drivers(status);
CREATE INDEX idx_driver_location ON drivers(latitude, longitude);
CREATE INDEX idx_driver_available ON drivers(status, latitude, longitude);
```

#### 2. rides
```sql
CREATE TABLE rides (
    id BIGSERIAL PRIMARY KEY,
    ride_id VARCHAR(255) UNIQUE NOT NULL,
    rider_id VARCHAR(255) NOT NULL,
    pickup_latitude DOUBLE PRECISION NOT NULL,
    pickup_longitude DOUBLE PRECISION NOT NULL,
    destination_latitude DOUBLE PRECISION NOT NULL,
    destination_longitude DOUBLE PRECISION NOT NULL,
    pickup_address VARCHAR(500),
    destination_address VARCHAR(500),
    tier VARCHAR(20) NOT NULL,
    payment_method VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    driver_id BIGINT,
    trip_id BIGINT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    matched_at TIMESTAMP,
    accepted_at TIMESTAMP,
    idempotency_key VARCHAR(255) UNIQUE
);

CREATE INDEX idx_ride_status ON rides(status);
CREATE INDEX idx_ride_rider ON rides(rider_id);
CREATE INDEX idx_ride_driver ON rides(driver_id);
CREATE INDEX idx_ride_created ON rides(created_at);
```

#### 3. trips
```sql
CREATE TABLE trips (
    id BIGSERIAL PRIMARY KEY,
    trip_id VARCHAR(255) UNIQUE NOT NULL,
    ride_id BIGINT NOT NULL,
    driver_id BIGINT NOT NULL,
    rider_id VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL,
    start_latitude DOUBLE PRECISION NOT NULL,
    start_longitude DOUBLE PRECISION NOT NULL,
    end_latitude DOUBLE PRECISION NOT NULL,
    end_longitude DOUBLE PRECISION NOT NULL,
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    pause_start_time TIMESTAMP,
    total_pause_duration_seconds BIGINT,
    distance_km DOUBLE PRECISION,
    duration_minutes DOUBLE PRECISION,
    base_fare DOUBLE PRECISION,
    distance_fare DOUBLE PRECISION,
    time_fare DOUBLE PRECISION,
    surge_multiplier DOUBLE PRECISION,
    total_fare DOUBLE PRECISION,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_trip_ride ON trips(ride_id);
CREATE INDEX idx_trip_status ON trips(status);
CREATE INDEX idx_trip_driver ON trips(driver_id);
```

#### 4. payments
```sql
CREATE TABLE payments (
    id BIGSERIAL PRIMARY KEY,
    payment_id VARCHAR(255) UNIQUE NOT NULL,
    trip_id BIGINT NOT NULL,
    rider_id VARCHAR(255) NOT NULL,
    amount DOUBLE PRECISION NOT NULL,
    payment_method VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    psp_transaction_id VARCHAR(255),
    psp_response TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP
);

CREATE INDEX idx_payment_trip ON payments(trip_id);
CREATE INDEX idx_payment_status ON payments(status);
CREATE INDEX idx_payment_rider ON payments(rider_id);
```

---

## API Design

### Core APIs

#### 1. POST /v1/rides
Create a new ride request.

**Request:**
```json
{
  "riderId": "RIDER-1",
  "pickupLatitude": 28.7041,
  "pickupLongitude": 77.1025,
  "destinationLatitude": 28.5355,
  "destinationLongitude": 77.3910,
  "pickupAddress": "Connaught Place, New Delhi",
  "destinationAddress": "India Gate, New Delhi",
  "tier": "ECONOMY",
  "paymentMethod": "CARD",
  "idempotencyKey": "unique-key-123"
}
```

**Response:**
```json
{
  "rideId": "RIDE-1234567890",
  "riderId": "RIDER-1",
  "status": "MATCHED",
  "driverId": 1,
  "tripId": null,
  "createdAt": "2024-01-15T10:30:00",
  "matchedAt": "2024-01-15T10:30:01",
  "acceptedAt": null
}
```

#### 2. GET /v1/rides/{id}
Get ride status.

**Response:**
```json
{
  "rideId": "RIDE-1234567890",
  "riderId": "RIDER-1",
  "status": "IN_PROGRESS",
  "driverId": 1,
  "tripId": 100,
  "createdAt": "2024-01-15T10:30:00",
  "matchedAt": "2024-01-15T10:30:01",
  "acceptedAt": "2024-01-15T10:30:05"
}
```

#### 3. POST /v1/drivers/{id}/location
Update driver location.

**Request:**
```json
{
  "latitude": 28.7041,
  "longitude": 77.1025
}
```

#### 4. POST /v1/drivers/{id}/accept
Accept ride assignment.

**Query Parameters:**
- `rideId`: Long (required)

#### 5. POST /v1/trips/{id}/end
End trip and calculate fare.

**Query Parameters:**
- `endLatitude`: Double (optional)
- `endLongitude`: Double (optional)

**Response:**
```json
{
  "id": 100,
  "tripId": "TRIP-1234567890",
  "rideId": 50,
  "driverId": 1,
  "riderId": "RIDER-1",
  "status": "COMPLETED",
  "distanceKm": 12.5,
  "durationMinutes": 25.0,
  "totalFare": 150.0,
  "endTime": "2024-01-15T11:00:00"
}
```

#### 6. POST /v1/payments
Process payment for a trip.

**Request:**
```json
{
  "tripId": 100,
  "idempotencyKey": "payment-key-123"
}
```

**Response:**
```json
{
  "paymentId": "PAY-1234567890",
  "tripId": 100,
  "amount": 150.0,
  "status": "SUCCESS",
  "pspTransactionId": "PSP-TXN-123",
  "createdAt": "2024-01-15T11:01:00"
}
```

---

## Scalability & Performance

### Performance Targets

- **Driver Matching**: p95 < 1 second
- **Location Updates**: Handle 200k updates/second
- **Ride Requests**: Handle 10k requests/minute
- **API Latency**: p95 < 500ms for read operations

### Optimization Strategies

1. **Database Indexing**
   - Composite indexes on (status, latitude, longitude) for driver queries
   - Indexes on foreign keys (ride_id, driver_id, trip_id)
   - Indexes on frequently queried fields (status, created_at)

2. **Caching**
   - Redis cache for ride status (5 min TTL)
   - Redis cache for available drivers by location (5 min TTL)
   - Redis cache for driver locations (5 sec TTL)
   - Cache invalidation on writes

3. **Query Optimization**
   - Use bounding box queries instead of distance calculations in SQL
   - Batch operations for bulk updates
   - Connection pooling (HikariCP)

4. **Concurrency Handling**
   - Optimistic locking for driver assignment
   - Database transactions for critical operations
   - Idempotency keys for retries

5. **Horizontal Scaling**
   - Stateless services enable load balancing
   - Database read replicas for read-heavy operations
   - Redis cluster for high availability

---

## Security

### Security Measures

1. **API Security**
   - Input validation on all endpoints
   - Rate limiting (to be implemented)
   - CORS configuration

2. **Data Security**
   - Parameterized queries (JPA prevents SQL injection)
   - Sensitive data encryption (to be implemented)
   - Secure communication (HTTPS)

3. **Authentication & Authorization**
   - API keys / JWT tokens (to be implemented)
   - Role-based access control (to be implemented)

---

## Monitoring & Observability

### New Relic Integration

**Note**: New Relic integration will be added separately. The following metrics should be tracked:

1. **API Performance**
   - Request latency (p50, p95, p99)
   - Request rate
   - Error rate
   - Response time by endpoint

2. **Database Performance**
   - Query execution time
   - Slow query detection
   - Connection pool metrics
   - Transaction duration

3. **Cache Performance**
   - Cache hit/miss ratio
   - Redis connection pool metrics
   - Cache eviction rate

4. **Business Metrics**
   - Ride requests per minute
   - Driver matching success rate
   - Average matching time
   - Payment success rate

### Logging

- Structured logging with correlation IDs
- Log levels: INFO, WARN, ERROR
- Request/response logging for debugging
- Exception stack traces

---

## Deployment

### Environment Setup

1. **Database**: PostgreSQL 14+
2. **Cache**: Redis 7+
3. **Application**: Java 17, Spring Boot 3.2.0
4. **Build**: Gradle 8+

### Configuration

- `application.properties`: Database and Redis connection settings
- Environment-specific configs (dev, staging, prod)
- Secrets management (to be implemented)

---

## Future Enhancements

1. **Message Queue**: Kafka/RabbitMQ for async processing
2. **WebSocket**: Real-time updates for ride status
3. **Surge Pricing**: Dynamic pricing based on demand
4. **Multi-region**: Cross-region replication and failover
5. **Advanced Matching**: ML-based driver-rider matching
6. **Notifications**: SMS/Email/Push notifications
7. **Analytics**: Real-time dashboards and reporting

---

## Testing Strategy

1. **Unit Tests**: Service layer and business logic
2. **Integration Tests**: API endpoints with test containers
3. **Performance Tests**: Load testing for scalability
4. **End-to-End Tests**: Complete ride flow

---

## Conclusion

This architecture provides a scalable, performant foundation for the ride-hailing platform. The system is designed to handle high traffic with sub-second response times while maintaining data consistency and reliability.

