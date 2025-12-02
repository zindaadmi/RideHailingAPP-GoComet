# GoComet DAW - Ride Hailing Application

A multi-tenant, multi-region ride-hailing platform built with Spring Boot, designed to handle high-scale traffic with sub-second response times.

## Features

- ✅ Real-time driver location updates (1-2 per second)
- ✅ Driver-rider matching within 1s p95
- ✅ Complete trip lifecycle management
- ✅ Payment processing via external PSPs
- ✅ Redis caching for performance
- ✅ Database indexing for fast queries
- ✅ Idempotent APIs
- ✅ Transaction management for consistency
- ✅ Comprehensive error handling
- ✅ **Simple Web Frontend** - HTML/CSS/JS interface for testing

## Technology Stack

- **Framework**: Spring Boot 3.2.0
- **Language**: Java 17
- **Database**: PostgreSQL (primary), H2 (testing)
- **Cache**: Redis
- **Build Tool**: Gradle
- **Validation**: Jakarta Validation

## Prerequisites

- Java 17 or higher
- PostgreSQL 14+ (optional - H2 in-memory database is used by default)
- Redis 7+ (optional - application works without Redis, but caching will be disabled)
- Gradle 8+

## Setup Instructions

### 1. Clone the Repository

```bash
git clone <repository-url>
cd GoComet-DAW
```

### 2. Database Setup

#### H2 (Default - Development)

The application uses H2 in-memory database by default. **No setup required!** Just run the application.

**Note**: Data will be reset when the application restarts. For persistent data, use PostgreSQL.

#### PostgreSQL (Production)

If you want to use PostgreSQL:

1. Create database:
```bash
createdb gocomet_daw
```

2. Update `application.properties`:
   - Comment out H2 configuration
   - Uncomment PostgreSQL configuration
   - Update database credentials

### 3. Redis Setup (Optional)

Redis is optional. The application will work without it, but caching will be disabled.

If you want to use Redis:

```bash
# Install Redis (macOS)
brew install redis

# Start Redis
redis-server

# Or use Docker
docker run -d -p 6379:6379 redis:7
```

**Note**: If Redis is not running, the application will still start successfully.

### 4. Configure Application (Optional)

The application is pre-configured to work out of the box with H2 database. No configuration needed!

If you want to use PostgreSQL or Redis, update `src/main/resources/application.properties` accordingly.

### 5. Build and Run

```bash
# Build the project
./gradlew build

# Run the application
./gradlew bootRun

# Or use the JAR
java -jar build/libs/GoComet-DAW-0.0.1-SNAPSHOT.jar
```

The application will start on `http://localhost:8080`

### 6. Access the Web Interface

Once the application is running, open your browser and navigate to:

```
http://localhost:8080
```

You'll see a simple web interface where you can:
- Create ride requests
- View real-time ride status updates
- Update driver locations
- See activity logs

**Note**: The frontend uses H2 in-memory database by default, so data will be reset on restart. For persistent data, configure PostgreSQL.

## API Endpoints

### Ride Management

#### Create Ride Request
```bash
POST /v1/rides
Content-Type: application/json

{
  "riderId": "RIDER-1",
  "pickupLatitude": 28.7041,
  "pickupLongitude": 77.1025,
  "destinationLatitude": 28.5355,
  "destinationLongitude": 77.3910,
  "tier": "ECONOMY",
  "paymentMethod": "CARD",
  "idempotencyKey": "unique-key-123"
}
```

#### Get Ride Status
```bash
GET /v1/rides/{rideId}
```

### Driver Management

#### Update Driver Location
```bash
POST /v1/drivers/{driverId}/location
Content-Type: application/json

{
  "latitude": 28.7041,
  "longitude": 77.1025
}
```

#### Accept Ride
```bash
POST /v1/drivers/{driverId}/accept?rideId=1
```

### Trip Management

#### End Trip
```bash
POST /v1/trips/{tripId}/end?endLatitude=28.5355&endLongitude=77.3910
```

### Payment

#### Process Payment
```bash
POST /v1/payments
Content-Type: application/json

{
  "tripId": 100,
  "idempotencyKey": "payment-key-123"
}
```

## Testing

### Run Unit Tests

```bash
./gradlew test
```

### Run Integration Tests

```bash
./gradlew integrationTest
```

## Project Structure

```
GoComet-DAW/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/interview/gocomet/GoComet/DAW/
│   │   │       ├── controller/      # REST Controllers
│   │   │       ├── service/         # Business Logic
│   │   │       ├── repository/      # Data Access
│   │   │       ├── model/           # Domain Entities
│   │   │       ├── dto/             # DTOs
│   │   │       ├── config/          # Configuration
│   │   │       └── exception/       # Exception Handlers
│   │   └── resources/
│   │       └── application.properties
│   └── test/
│       └── java/                    # Test Classes
├── build.gradle
├── ARCHITECTURE.md                  # Architecture Documentation
└── README.md
```

## Performance Optimizations

1. **Database Indexing**: Composite indexes on frequently queried fields
2. **Redis Caching**: Hot data cached with appropriate TTLs
3. **Query Optimization**: Bounding box queries for spatial searches
4. **Connection Pooling**: HikariCP for efficient database connections
5. **Transaction Management**: Optimistic locking for concurrency

## Monitoring

### New Relic Integration

**Note**: New Relic integration will be added separately. To integrate:

1. Sign up for New Relic account (100GB free tier)
2. Add New Relic Java agent dependency
3. Configure New Relic license key in `application.properties`
4. Monitor API latencies, database queries, and cache performance

### Metrics to Track

- API response times (p50, p95, p99)
- Database query performance
- Cache hit/miss ratios
- Error rates
- Business metrics (rides/min, matching success rate)

## Development Guidelines

1. **Code Style**: Follow Java conventions and Spring Boot best practices
2. **Testing**: Write unit tests for all services
3. **Documentation**: Update API documentation for new endpoints
4. **Error Handling**: Use GlobalExceptionHandler for consistent error responses
5. **Logging**: Use structured logging with appropriate log levels

## Troubleshooting

### Database Connection Issues

- Verify PostgreSQL is running: `pg_isready`
- Check database credentials in `application.properties`
- Ensure database exists: `createdb gocomet_daw`

### Redis Connection Issues

- Verify Redis is running: `redis-cli ping`
- Check Redis host/port in `application.properties`
- Test connection: `redis-cli -h localhost -p 6379`

### Build Issues

- Clean and rebuild: `./gradlew clean build`
- Check Java version: `java -version` (should be 17+)
- Verify Gradle version: `./gradlew --version`

## Contributing

1. Create a feature branch
2. Make your changes
3. Write/update tests
4. Submit a pull request

## License

This project is part of the GoComet DAW interview process.

## Contact

For questions or issues, please contact the development team.

