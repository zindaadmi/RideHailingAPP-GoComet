# GoComet DAW - Complete API Documentation

## ✅ All Required APIs Implemented

### Core Required APIs (6 endpoints)

#### 1. **POST /v1/rides** - Create Ride Request
- **Description**: Creates a new ride request and matches a driver
- **Request Body**: 
  ```json
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
- **Response**: `201 Created` with `RideResponse`
- **Features**: 
  - Automatic driver matching
  - Idempotency support
  - Surge pricing calculation

#### 2. **GET /v1/rides/{id}** - Get Ride Status
- **Description**: Retrieves current status of a ride
- **Path Parameter**: `id` (String) - Ride ID
- **Response**: `200 OK` with `RideResponse`
- **Features**: 
  - Cached response for performance
  - Returns 404 if not found

#### 3. **POST /v1/drivers/{id}/location** - Update Driver Location
- **Description**: Updates driver's current location
- **Path Parameter**: `id` (String) - Driver ID
- **Request Body**:
  ```json
  {
    "latitude": 28.7041,
    "longitude": 77.1025
  }
  ```
- **Response**: `200 OK` with `Driver` object
- **Features**: 
  - Real-time location caching in Redis
  - Updates driver availability cache

#### 4. **POST /v1/drivers/{id}/accept** - Accept Ride Assignment
- **Description**: Driver accepts a ride assignment
- **Path Parameter**: `id` (String) - Driver ID
- **Query Parameter**: `rideId` (Long) - Required
- **Response**: `200 OK`
- **Features**: 
  - Automatically starts trip when driver accepts
  - Updates ride status to ACCEPTED
  - Changes ride status to IN_PROGRESS

#### 5. **POST /v1/trips/{id}/end** - End Trip
- **Description**: Ends a trip and calculates fare
- **Path Parameter**: `id` (Long) - Trip ID
- **Query Parameters**: 
  - `endLatitude` (Double, optional)
  - `endLongitude` (Double, optional)
- **Response**: `200 OK` with `Trip` object including calculated fare
- **Features**: 
  - Distance calculation
  - Duration calculation
  - Fare calculation with surge pricing
  - Updates trip status to COMPLETED

#### 6. **POST /v1/payments** - Process Payment
- **Description**: Processes payment for a completed trip
- **Request Body**:
  ```json
  {
    "tripId": 100,
    "idempotencyKey": "payment-key-123"
  }
  ```
- **Response**: `201 Created` with `PaymentResponse`
- **Features**: 
  - Idempotency support
  - External PSP integration (simulated)
  - Payment status tracking

---

## Additional Useful APIs (5 endpoints)

#### 7. **GET /v1/trips/{id}** - Get Trip Details
- **Description**: Retrieves trip information
- **Path Parameter**: `id` (Long) - Trip ID
- **Response**: `200 OK` with `Trip` object
- **Features**: 
  - Complete trip details including fare, distance, duration

#### 8. **GET /v1/payments/{id}** - Get Payment Status
- **Description**: Retrieves payment information by payment ID
- **Path Parameter**: `id` (String) - Payment ID
- **Response**: `200 OK` with `PaymentResponse`
- **Features**: 
  - Payment status tracking
  - PSP transaction ID

#### 9. **GET /v1/drivers/{id}** - Get Driver Details
- **Description**: Retrieves driver information
- **Path Parameter**: `id` (String) - Driver ID
- **Response**: `200 OK` with `Driver` object
- **Features**: 
  - Current driver status
  - Location information

#### 10. **POST /v1/trips/{id}/start** - Start Trip (Manual)
- **Description**: Manually start a trip (usually automatic on driver accept)
- **Path Parameter**: `id` (Long) - Ride ID
- **Response**: `201 Created` with `Trip` object
- **Features**: 
  - Creates trip record
  - Links trip to ride

#### 11. **POST /v1/trips/{id}/pause** - Pause Trip
- **Description**: Pauses an ongoing trip
- **Path Parameter**: `id` (Long) - Trip ID
- **Response**: `200 OK` with `Trip` object
- **Features**: 
  - Updates trip status to PAUSED
  - Stops fare calculation temporarily

#### 12. **POST /v1/trips/{id}/resume** - Resume Trip
- **Description**: Resumes a paused trip
- **Path Parameter**: `id` (Long) - Trip ID
- **Response**: `200 OK` with `Trip` object
- **Features**: 
  - Updates trip status back to IN_PROGRESS
  - Resumes fare calculation

---

## API Summary

### Total Endpoints: **12**
- **Required APIs**: 6 ✅
- **Additional APIs**: 6 ✅

### All APIs Include:
- ✅ Proper HTTP status codes
- ✅ JSON request/response bodies
- ✅ Error handling with JSON error responses
- ✅ Input validation using `@Valid`
- ✅ Idempotency support (where applicable)
- ✅ Comprehensive logging
- ✅ Transaction management

### Error Handling
All APIs return consistent error responses:
```json
{
  "error": "Error message description"
}
```

### Status Codes
- `200 OK` - Successful GET/POST/PUT
- `201 Created` - Resource created successfully
- `400 Bad Request` - Invalid request/state
- `404 Not Found` - Resource not found
- `500 Internal Server Error` - Server error

---

## Testing the APIs

### Quick Test Flow

1. **Create a Ride**:
   ```bash
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
       "idempotencyKey": "test-123"
     }'
   ```

2. **Get Ride Status**:
   ```bash
   curl http://localhost:8080/v1/rides/RIDE-1234567890
   ```

3. **Update Driver Location**:
   ```bash
   curl -X POST http://localhost:8080/v1/drivers/DRIVER-1/location \
     -H "Content-Type: application/json" \
     -d '{"latitude": 28.7041, "longitude": 77.1025}'
   ```

4. **Driver Accepts Ride**:
   ```bash
   curl -X POST "http://localhost:8080/v1/drivers/DRIVER-1/accept?rideId=1"
   ```

5. **End Trip**:
   ```bash
   curl -X POST "http://localhost:8080/v1/trips/1/end?endLatitude=28.5355&endLongitude=77.3910"
   ```

6. **Process Payment**:
   ```bash
   curl -X POST http://localhost:8080/v1/payments \
     -H "Content-Type: application/json" \
     -d '{"tripId": 1, "idempotencyKey": "pay-123"}'
   ```

---

## Frontend Integration

All APIs are integrated with the web dashboard at `http://localhost:8080`:
- Ride request form
- Real-time status updates
- Driver location updates
- Trip management
- Payment processing

---

## Status: ✅ COMPLETE

All required APIs are fully implemented and tested. The application is ready for use!

