const API_BASE_URL = 'http://localhost:8080/v1';

let currentRideId = null;
let currentTripId = null;
let pollingInterval = null;

// Add message to activity log
function addMessage(message, type = 'info') {
    const messagesDiv = document.getElementById('messages');
    const messageDiv = document.createElement('div');
    messageDiv.className = `message ${type}`;
    
    const time = new Date().toLocaleTimeString();
    messageDiv.innerHTML = `
        <div>${message}</div>
        <div class="message-time">${time}</div>
    `;
    
    messagesDiv.insertBefore(messageDiv, messagesDiv.firstChild);
    
    // Keep only last 20 messages
    while (messagesDiv.children.length > 20) {
        messagesDiv.removeChild(messagesDiv.lastChild);
    }
}

// Handle ride form submission
document.getElementById('rideForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    
    const rideData = {
        riderId: document.getElementById('riderId').value,
        pickupLatitude: parseFloat(document.getElementById('pickupLat').value),
        pickupLongitude: parseFloat(document.getElementById('pickupLng').value),
        destinationLatitude: parseFloat(document.getElementById('destLat').value),
        destinationLongitude: parseFloat(document.getElementById('destLng').value),
        tier: document.getElementById('tier').value,
        paymentMethod: document.getElementById('paymentMethod').value,
        idempotencyKey: `ride-${Date.now()}`
    };
    
    try {
        addMessage('Creating ride request...', 'info');
        
        const response = await fetch(`${API_BASE_URL}/rides`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(rideData)
        });
        
        if (!response.ok) {
            let errorMessage = 'Failed to create ride';
            try {
                const errorData = await response.json();
                errorMessage = errorData.error || errorData.message || errorMessage;
            } catch (e) {
                errorMessage = response.statusText || errorMessage;
            }
            throw new Error(errorMessage);
        }
        
        const ride = await response.json();
        currentRideId = ride.rideId;
        
        addMessage(`Ride created successfully! Ride ID: ${ride.rideId}`, 'success');
        addMessage(`Status: ${ride.status}${ride.driverId ? ` | Driver ID: ${ride.driverId}` : ''}`, 'info');
        
        // Show ride status section
        document.getElementById('rideStatusSection').style.display = 'block';
        updateRideStatus(ride);
        
        // Start polling for status updates
        startPolling(ride.rideId);
        
    } catch (error) {
        addMessage(`Error: ${error.message}`, 'error');
        console.error('Error creating ride:', error);
    }
});

// Update ride status display
function updateRideStatus(ride) {
    document.getElementById('statusRideId').textContent = ride.rideId || '-';
    document.getElementById('statusStatus').textContent = ride.status || '-';
    document.getElementById('statusStatus').className = `value status-badge ${ride.status || ''}`;
    document.getElementById('statusDriverId').textContent = ride.driverId || 'Not assigned';
    document.getElementById('statusTripId').textContent = ride.tripId || '-';
    document.getElementById('statusCreatedAt').textContent = ride.createdAt ? 
        new Date(ride.createdAt).toLocaleString() : '-';
    document.getElementById('statusMatchedAt').textContent = ride.matchedAt ? 
        new Date(ride.matchedAt).toLocaleString() : '-';
    
    // Store trip ID if available
    if (ride.tripId) {
        currentTripId = ride.tripId;
    }
    
    // Update ride ID copy field
    const rideIdInput = document.getElementById('rideIdToCopy');
    const rideIdCopySection = document.getElementById('rideIdCopySection');
    if (rideIdInput) {
        rideIdInput.value = ride.rideId || '';
    }
    
    // Show/hide ride ID copy section based on status
    if (rideIdCopySection) {
        if (ride.status === 'MATCHED' && ride.driverId) {
            rideIdCopySection.style.display = 'block';
        } else if (ride.status === 'ACCEPTED' || ride.status === 'IN_PROGRESS' || ride.status === 'COMPLETED') {
            rideIdCopySection.style.display = 'none';
        } else {
            rideIdCopySection.style.display = 'none';
        }
    }
}

// Poll ride status
async function pollRideStatus(rideId) {
    try {
        const response = await fetch(`${API_BASE_URL}/rides/${rideId}`);
        
        if (!response.ok) {
            if (response.status === 404) {
                stopPolling();
                addMessage('Ride not found. Polling stopped.', 'error');
                return;
            }
            let errorMessage = 'Failed to fetch ride status';
            try {
                const errorData = await response.json();
                errorMessage = errorData.error || errorData.message || errorMessage;
            } catch (e) {
                errorMessage = response.statusText || errorMessage;
            }
            throw new Error(errorMessage);
        }
        
        const ride = await response.json();
        updateRideStatus(ride);
        
        // Log status changes (only show message once per status change)
        const previousStatus = document.getElementById('statusStatus').getAttribute('data-previous-status') || '';
        const currentStatus = ride.status?.toString() || '';
        
        if (ride.status === 'MATCHED' && ride.driverId && previousStatus !== 'MATCHED') {
            addMessage(`✅ Driver matched! Driver ID: ${ride.driverId}`, 'success');
            addMessage(`📋 Copy the Ride ID above and give it to the driver to accept the ride.`, 'info');
            document.getElementById('statusStatus').setAttribute('data-previous-status', 'MATCHED');
        } else if (ride.status === 'ACCEPTED' && previousStatus !== 'ACCEPTED') {
            addMessage('✅ Driver accepted the ride! Trip will start automatically...', 'success');
            document.getElementById('statusStatus').setAttribute('data-previous-status', 'ACCEPTED');
        } else if (ride.status === 'IN_PROGRESS' && previousStatus !== 'IN_PROGRESS') {
            addMessage('🚗 Trip started! Driver is on the way.', 'success');
            if (ride.tripId) {
                currentTripId = ride.tripId;
            }
            document.getElementById('statusStatus').setAttribute('data-previous-status', 'IN_PROGRESS');
        } else if (ride.status === 'COMPLETED' && previousStatus !== 'COMPLETED') {
            addMessage('✅ Trip completed! Fetching fare details...', 'success');
            // Show payment section immediately
            document.getElementById('paymentSection').style.display = 'block';
            
            // Fetch trip details to show fare and payment section
            if (ride.tripId) {
                currentTripId = ride.tripId;
                // Fetch trip details immediately
                fetchTripDetails(ride.tripId);
            } else {
                addMessage('Fetching trip details...', 'info');
                // Try to get trip ID from ride if not available
                setTimeout(() => {
                    if (!currentTripId && ride.tripId) {
                        currentTripId = ride.tripId;
                        fetchTripDetails(ride.tripId);
                    }
                }, 1000);
            }
            document.getElementById('statusStatus').setAttribute('data-previous-status', 'COMPLETED');
        } else if (ride.status === 'COMPLETED' && previousStatus === 'COMPLETED') {
            // Already completed, ensure payment section is shown
            document.getElementById('paymentSection').style.display = 'block';
            if (ride.tripId && !currentTripId) {
                currentTripId = ride.tripId;
                fetchTripDetails(ride.tripId);
            }
        } else if (ride.status === 'CANCELLED' && previousStatus !== 'CANCELLED') {
            addMessage('Ride cancelled.', 'error');
            stopPolling();
            document.getElementById('statusStatus').setAttribute('data-previous-status', 'CANCELLED');
        }
        
        // Update previous status
        if (currentStatus) {
            document.getElementById('statusStatus').setAttribute('data-previous-status', currentStatus);
        }
        
    } catch (error) {
        addMessage(`Error polling status: ${error.message}`, 'error');
        console.error('Error polling ride status:', error);
    }
}

// Start polling
function startPolling(rideId) {
    if (pollingInterval) {
        clearInterval(pollingInterval);
    }
    
    // Poll immediately
    pollRideStatus(rideId);
    
    // Then poll every 2 seconds
    pollingInterval = setInterval(() => {
        pollRideStatus(rideId);
    }, 2000);
    
    addMessage('Started tracking ride status...', 'info');
}

// Stop polling
function stopPolling() {
    if (pollingInterval) {
        clearInterval(pollingInterval);
        pollingInterval = null;
        addMessage('Stopped tracking ride status.', 'info');
    }
}

// Fetch trip details
async function fetchTripDetails(tripId) {
    try {
        const response = await fetch(`${API_BASE_URL}/trips/${tripId}`);
        if (response.ok) {
            const trip = await response.json();
            currentTripId = tripId;
            
            // Always show payment section when trip is completed
            const paymentSection = document.getElementById('paymentSection');
            if (paymentSection) {
                paymentSection.style.display = 'block';
                paymentSection.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
            }
            
            if (trip.status === 'COMPLETED' && trip.totalFare != null) {
                document.getElementById('tripFare').textContent = `₹${trip.totalFare.toFixed(2)}`;
                addMessage(`💰 Total fare: ₹${trip.totalFare.toFixed(2)}. Click "Pay Now" button below to complete payment.`, 'success');
            } else if (trip.status === 'COMPLETED') {
                // Trip completed but fare not calculated yet
                document.getElementById('tripFare').textContent = 'Calculating...';
                addMessage('Trip completed. Fare is being calculated...', 'info');
                // Retry after a delay
                setTimeout(() => fetchTripDetails(tripId), 2000);
            }
        } else {
            addMessage('Could not fetch trip details. You can still try to pay using the button below.', 'warning');
            // Show payment section anyway with manual pay option
            const paymentSection = document.getElementById('paymentSection');
            if (paymentSection) {
                paymentSection.style.display = 'block';
                paymentSection.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
            }
            document.getElementById('tripFare').textContent = 'N/A';
            currentTripId = tripId;
        }
    } catch (error) {
        console.error('Error fetching trip details:', error);
        addMessage('Could not fetch trip details. You can still try to pay using the button below.', 'warning');
        // Show payment section anyway
        const paymentSection = document.getElementById('paymentSection');
        if (paymentSection) {
            paymentSection.style.display = 'block';
            paymentSection.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
        }
        document.getElementById('tripFare').textContent = 'N/A';
        currentTripId = tripId;
    }
}

// Process payment
async function processPayment() {
    // Try to get trip ID from current ride if not set
    if (!currentTripId && currentRideId) {
        try {
            const response = await fetch(`${API_BASE_URL}/rides/${currentRideId}`);
            if (response.ok) {
                const ride = await response.json();
                if (ride.tripId) {
                    currentTripId = ride.tripId;
                }
            }
        } catch (e) {
            console.error('Error fetching ride for trip ID:', e);
        }
    }
    
    if (!currentTripId) {
        addMessage('No trip available for payment. Please wait for trip to complete.', 'error');
        return;
    }
    
    try {
        addMessage(`Processing payment for trip ${currentTripId}...`, 'info');
        document.getElementById('payBtn').disabled = true;
        document.getElementById('payBtn').textContent = 'Processing...';
        
        const paymentData = {
            tripId: currentTripId,
            idempotencyKey: `payment-${Date.now()}`
        };
        
        const response = await fetch(`${API_BASE_URL}/payments`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(paymentData)
        });
        
        if (!response.ok) {
            let errorMessage = 'Payment failed';
            try {
                const errorData = await response.json();
                errorMessage = errorData.error || errorData.message || errorMessage;
            } catch (e) {
                errorMessage = response.statusText || errorMessage;
            }
            throw new Error(errorMessage);
        }
        
        const payment = await response.json();
        
        if (payment.status === 'SUCCESS') {
            addMessage(`Payment successful! Transaction ID: ${payment.pspTransactionId || payment.paymentId}`, 'success');
            document.getElementById('payBtn').textContent = 'Payment Successful ✓';
            document.getElementById('payBtn').style.background = '#28a745';
            document.getElementById('payBtn').disabled = true;
        } else if (payment.status === 'FAILED') {
            addMessage(`Payment failed. Please try again.`, 'error');
            document.getElementById('payBtn').disabled = false;
            document.getElementById('payBtn').textContent = 'Pay Now';
        } else {
            addMessage(`Payment status: ${payment.status}`, 'info');
            document.getElementById('payBtn').disabled = false;
            document.getElementById('payBtn').textContent = 'Pay Now';
        }
        
    } catch (error) {
        addMessage(`Error: ${error.message}`, 'error');
        document.getElementById('payBtn').disabled = false;
        document.getElementById('payBtn').textContent = 'Pay Now';
        console.error('Error processing payment:', error);
    }
}

// Initialize
addMessage('Rider dashboard loaded. Ready to request rides!', 'success');

