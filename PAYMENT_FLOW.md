# Payment Flow - How to Pay After Trip Ends

## Complete Flow

### Step 1: Request a Ride
1. Fill in ride details (pickup, destination, tier, payment method)
2. Click "Request Ride"
3. System matches a driver automatically

### Step 2: Driver Accepts (Automatic)
- When driver accepts, trip starts automatically
- Ride status changes to "IN_PROGRESS"
- "End Trip" button appears

### Step 3: End the Trip
1. Click "End Trip" button (appears when trip is IN_PROGRESS)
2. System calculates fare based on:
   - Distance traveled
   - Time taken
   - Base fare
   - Surge multiplier
3. Trip status changes to "COMPLETED"
4. Payment section appears with total fare

### Step 4: Make Payment
1. Click "Pay Now" button
2. System processes payment via external PSP
3. Payment status is displayed:
   - ✅ **SUCCESS**: Payment completed
   - ❌ **FAILED**: Payment failed (can retry)
   - ⏳ **PROCESSING**: Payment in progress

## API Flow

### 1. End Trip
```bash
POST /v1/trips/{tripId}/end
```

**Response:**
```json
{
  "id": 1,
  "tripId": "TRIP-123",
  "status": "COMPLETED",
  "totalFare": 150.0,
  "distanceKm": 12.5,
  "durationMinutes": 25.0
}
```

### 2. Process Payment
```bash
POST /v1/payments
Content-Type: application/json

{
  "tripId": 1,
  "idempotencyKey": "payment-123"
}
```

**Response:**
```json
{
  "paymentId": "PAY-123",
  "tripId": 1,
  "amount": 150.0,
  "status": "SUCCESS",
  "pspTransactionId": "PSP-TXN-123"
}
```

## Frontend Flow

1. **Ride Created** → Status: PENDING/MATCHED
2. **Driver Accepts** → Status: ACCEPTED → Trip starts automatically → Status: IN_PROGRESS
3. **"End Trip" button appears** → Click to end trip
4. **Trip ends** → Fare calculated → "Pay Now" button appears
5. **Click "Pay Now"** → Payment processed → Success/Failed message

## Payment Methods Supported

- **CASH**: Cash payment (processed but no PSP call)
- **CARD**: Credit/Debit card
- **UPI**: UPI payment
- **WALLET**: Digital wallet

## Fare Calculation

```
Base Fare: ₹25
+ Distance Fare: ₹8/km
+ Time Fare: ₹1/minute
× Surge Multiplier: 1.0-3.0
= Total Fare (minimum ₹40)
```

## Troubleshooting

### "End Trip" button not showing?
- Make sure ride status is "IN_PROGRESS"
- Check if tripId is available in ride status
- Refresh the page

### "Pay Now" button not showing?
- Trip must be completed first (click "End Trip")
- Check if fare is calculated (should show total fare)

### Payment failed?
- Click "Pay Now" again to retry
- Check payment method is valid
- Payment uses idempotency, so retries are safe

## Quick Test Flow

1. **Create Ride**: Use default values, click "Request Ride"
2. **Wait for Match**: Driver should match automatically (5 sample drivers created)
3. **Accept Ride**: Driver accepts automatically (or manually via API)
4. **End Trip**: Click "End Trip" button when it appears
5. **Pay**: Click "Pay Now" button
6. **Done**: Payment success message appears

---

**Note**: The trip starts automatically when driver accepts the ride. You just need to end it and pay!

