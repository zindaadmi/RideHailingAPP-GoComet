# When Will Payment Prompt Appear?

## Payment Flow Timeline

### Step-by-Step Flow:

1. **Request Ride** ✅
   - Click "Request Ride" button
   - Status: PENDING

2. **Driver Matched** ✅
   - System automatically matches a driver
   - Status: MATCHED
   - **Payment: ❌ Not yet**

3. **Driver Accepts** ✅
   - Driver accepts the ride (automatic or manual)
   - Trip starts automatically
   - Status: IN_PROGRESS
   - **"End Trip" button appears** 🟢
   - **Payment: ❌ Not yet - Need to end trip first**

4. **End Trip** ✅
   - Click "End Trip" button (appears when status is IN_PROGRESS)
   - System calculates fare
   - Status: COMPLETED
   - **Payment section appears** 🟢
   - **"Pay Now" button appears** 🟢

5. **Make Payment** ✅
   - Click "Pay Now" button
   - Payment is processed
   - Success/Failed message shown

## Visual Flow

```
Request Ride
    ↓
PENDING
    ↓
MATCHED (Driver found)
    ↓
ACCEPTED (Driver accepts)
    ↓
IN_PROGRESS (Trip started) ← "End Trip" button appears here
    ↓
[Click "End Trip"]
    ↓
COMPLETED (Fare calculated) ← "Pay Now" button appears here
    ↓
[Click "Pay Now"]
    ↓
Payment Processed
```

## When Payment Prompt Appears

**Payment prompt appears AFTER:**
1. ✅ Ride is created
2. ✅ Driver is matched
3. ✅ Driver accepts (trip starts automatically)
4. ✅ Ride status is "IN_PROGRESS"
5. ✅ **You click "End Trip" button**
6. ✅ **Payment section appears with "Pay Now" button**

## Important Notes

- **"End Trip" button** appears when:
  - Ride status is "IN_PROGRESS"
  - Trip ID is available

- **"Pay Now" button** appears when:
  - Trip is ended (status is COMPLETED)
  - Fare is calculated
  - Payment section is displayed

## Quick Answer

**Payment prompt appears AFTER you click "End Trip" button.**

The flow is:
1. Request ride → 2. Driver matches → 3. Trip starts → 4. **Click "End Trip"** → 5. **Payment prompt appears** → 6. Click "Pay Now"

## Testing

To see the payment prompt:
1. Create a ride
2. Wait for status to become "IN_PROGRESS"
3. Click "End Trip" button
4. Payment section will appear with "Pay Now" button

