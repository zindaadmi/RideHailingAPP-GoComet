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
document.addEventListener('DOMContentLoaded', () => {
    const rideForm = document.getElementById('rideForm');
    if (rideForm) {
        rideForm.addEventListener('submit', async (e) => {
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
        
        // IMPORTANT: Clear old trip/ride IDs when creating a new ride
        currentRideId = ride.rideId;
        currentTripId = null; // Clear old trip ID
        
        // Clear old payment success section if visible
        const paymentSuccessSection = document.getElementById('paymentSuccessSection');
        if (paymentSuccessSection) {
            paymentSuccessSection.style.display = 'none';
        }
        
        // Clear old payment section if visible
        const paymentSection = document.getElementById('paymentSection');
        if (paymentSection) {
            paymentSection.style.display = 'none';
        }
        
        // Clear activity log and show only new ride messages
        const messagesDiv = document.getElementById('messages');
        if (messagesDiv) {
            messagesDiv.innerHTML = '';
        }
        
        addMessage(`✅ Ride created successfully! Ride ID: ${ride.rideId}`, 'success');
        addMessage(`Status: ${ride.status}${ride.driverId ? ` | Driver ID: ${ride.driverId}` : ''}`, 'info');
        
        // Show ride status section
        const rideStatusSection = document.getElementById('rideStatusSection');
        if (rideStatusSection) {
            rideStatusSection.style.display = 'block';
        }
        updateRideStatus(ride);
        
        // Start polling for status updates
        startPolling(ride.rideId);
        
    } catch (error) {
        addMessage(`Error: ${error.message}`, 'error');
        console.error('Error creating ride:', error);
    }
        });
    }
    
    // Make functions globally accessible after DOM is loaded
    window.clearOldRideInfo = clearOldRideInfo;
    window.processPayment = processPayment;
    
    addMessage('Rider dashboard loaded. Ready to request rides!', 'success');
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

// Stop polling (internal function, no longer exposed)
function stopPolling() {
    if (pollingInterval) {
        clearInterval(pollingInterval);
        pollingInterval = null;
    }
}

// Clear old ride information and reset form
function clearOldRideInfo() {
    try {
        // Reset current ride/trip IDs
        currentRideId = null;
        currentTripId = null;
        
        // Stop any active polling
        if (pollingInterval) {
            clearInterval(pollingInterval);
            pollingInterval = null;
        }
        
        // Hide ride status section completely
        const rideStatusSection = document.getElementById('rideStatusSection');
        if (rideStatusSection) {
            rideStatusSection.style.display = 'none';
        }
        
        // Hide all payment sections
        const paymentSection = document.getElementById('paymentSection');
        if (paymentSection) {
            paymentSection.style.display = 'none';
        }
        
        const paymentSuccessSection = document.getElementById('paymentSuccessSection');
        if (paymentSuccessSection) {
            paymentSuccessSection.style.display = 'none';
        }
        
        // Clear ALL activity log messages
        const messagesDiv = document.getElementById('messages');
        if (messagesDiv) {
            messagesDiv.innerHTML = '';
        }
        
        // Reset form
        const rideForm = document.getElementById('rideForm');
        if (rideForm) {
            rideForm.reset();
            // Set default values safely
            const riderIdInput = document.getElementById('riderId');
            const pickupLatInput = document.getElementById('pickupLat');
            const pickupLngInput = document.getElementById('pickupLng');
            const destLatInput = document.getElementById('destLat');
            const destLngInput = document.getElementById('destLng');
            
            if (riderIdInput) riderIdInput.value = 'RIDER-1';
            if (pickupLatInput) pickupLatInput.value = '28.7041';
            if (pickupLngInput) pickupLngInput.value = '77.1025';
            if (destLatInput) destLatInput.value = '28.5355';
            if (destLngInput) destLngInput.value = '77.3910';
        }
        
        // Add a fresh message
        addMessage('✅ Ready for a new ride! Fill in the form above to request a new ride.', 'success');
        
        // Scroll to top to show the form
        window.scrollTo({ top: 0, behavior: 'smooth' });
    } catch (error) {
        console.error('Error clearing old ride info:', error);
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

// Handle payment success
function handlePaymentSuccess(payment) {
    // Stop polling immediately
    if (pollingInterval) {
        clearInterval(pollingInterval);
        pollingInterval = null;
    }
    
    // Reset current ride/trip IDs
    currentRideId = null;
    currentTripId = null;
    
    // Hide ride status section immediately
    const rideStatusSection = document.getElementById('rideStatusSection');
    if (rideStatusSection) {
        rideStatusSection.style.display = 'none';
    }
    
    // Hide payment section and show success section
    const paymentSection = document.getElementById('paymentSection');
    if (paymentSection) {
        paymentSection.style.display = 'none';
    }
    
    const paymentSuccessSection = document.getElementById('paymentSuccessSection');
    if (paymentSuccessSection) {
        paymentSuccessSection.style.display = 'block';
        
        // Update success section with payment details
        const tripFareElement = document.getElementById('tripFare');
        const fareAmount = payment.amount?.toFixed(2) || (tripFareElement ? tripFareElement.textContent.replace('₹', '') : '0.00');
        document.getElementById('paidAmount').textContent = `₹${fareAmount}`;
        document.getElementById('transactionId').textContent = payment.pspTransactionId || payment.paymentId || 'N/A';
    }
    
    // Clear ALL old activity log messages and show only payment success
    const messagesDiv = document.getElementById('messages');
    if (messagesDiv) {
        messagesDiv.innerHTML = ''; // Clear all messages
    }
    
    // Add only the payment success message
    addMessage(`✅ Payment successful! Transaction ID: ${payment.pspTransactionId || payment.paymentId}`, 'success');
    
    // Reset form
    const rideForm = document.getElementById('rideForm');
    if (rideForm) {
        rideForm.reset();
        // Set default values
        const riderIdInput = document.getElementById('riderId');
        const pickupLatInput = document.getElementById('pickupLat');
        const pickupLngInput = document.getElementById('pickupLng');
        const destLatInput = document.getElementById('destLat');
        const destLngInput = document.getElementById('destLng');
        
        if (riderIdInput) riderIdInput.value = 'RIDER-1';
        if (pickupLatInput) pickupLatInput.value = '28.7041';
        if (pickupLngInput) pickupLngInput.value = '77.1025';
        if (destLatInput) destLatInput.value = '28.5355';
        if (destLngInput) destLngInput.value = '77.3910';
    }
    
    // Scroll to success section
    if (paymentSuccessSection) {
        paymentSuccessSection.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
    }
}

// Handle payment failure
function handlePaymentFailure(payment) {
    addMessage(`❌ Payment failed. Please try again.`, 'error');
    const payBtn = document.getElementById('payBtn');
    if (payBtn) {
        payBtn.disabled = false;
        payBtn.textContent = '💳 Pay Now';
        payBtn.style.background = '';
    }
}

// Process payment
async function processPayment() {
    const payBtn = document.getElementById('payBtn');
    if (!payBtn) {
        addMessage('Payment button not found', 'error');
        return;
    }
    
    // Try to get trip ID from current ride if not set
    if (!currentTripId && currentRideId) {
        try {
            const response = await fetch(`${API_BASE_URL}/rides/${currentRideId}`);
            if (response.ok) {
                const ride = await response.json();
                if (ride.tripId) {
                    currentTripId = ride.tripId;
                } else {
                    addMessage('Trip has not started yet. Please wait for driver to accept and start the trip.', 'error');
                    return;
                }
            }
        } catch (e) {
            console.error('Error fetching ride for trip ID:', e);
            addMessage('Error fetching trip information. Please try again.', 'error');
            return;
        }
    }
    
    if (!currentTripId) {
        addMessage('No trip available for payment. Please wait for trip to complete.', 'error');
        return;
    }
    
    try {
        addMessage(`Processing payment for trip ${currentTripId}...`, 'info');
        payBtn.disabled = true;
        payBtn.textContent = 'Processing...';
        
        // Use unique idempotency key with trip ID to avoid conflicts
        const paymentData = {
            tripId: currentTripId,
            idempotencyKey: `payment-${currentTripId}-${Date.now()}`
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
        
        // Handle payment response - payment should return SUCCESS directly
        if (payment.status === 'SUCCESS') {
            handlePaymentSuccess(payment);
        } else if (payment.status === 'FAILED') {
            handlePaymentFailure(payment);
        } else if (payment.status === 'PROCESSING') {
            // If somehow PROCESSING is returned, wait and check again
            addMessage('Payment is processing, checking status...', 'info');
            setTimeout(async () => {
                try {
                    const checkResponse = await fetch(`${API_BASE_URL}/payments/${payment.paymentId}`);
                    if (checkResponse.ok) {
                        const updatedPayment = await checkResponse.json();
                        if (updatedPayment.status === 'SUCCESS') {
                            handlePaymentSuccess(updatedPayment);
                        } else {
                            handlePaymentFailure(updatedPayment);
                        }
                    } else {
                        handlePaymentFailure(payment);
                    }
                } catch (e) {
                    handlePaymentFailure(payment);
                }
            }, 500);
        } else {
            // Handle any other status
            addMessage(`⚠️ Payment status: ${payment.status}. Please try again.`, 'warning');
            payBtn.disabled = false;
            payBtn.textContent = '💳 Pay Now';
            payBtn.style.background = '';
        }
        
    } catch (error) {
        addMessage(`Error: ${error.message}`, 'error');
        payBtn.disabled = false;
        payBtn.textContent = '💳 Pay Now';
        payBtn.style.background = '';
        console.error('Error processing payment:', error);
    }
}

// Fetch and display active rides
async function fetchActiveRides() {
    try {
        const response = await fetch(`${API_BASE_URL}/rides/active`);
        if (!response.ok) {
            throw new Error('Failed to fetch active rides');
        }
        
        const rides = await response.json();
        displayActiveRides(rides);
    } catch (error) {
        console.error('Error fetching active rides:', error);
        const container = document.getElementById('activeRidesContainer');
        if (container) {
            container.innerHTML = '<p style="color: #dc3545;">Error loading active rides</p>';
        }
    }
}

// Display active rides
function displayActiveRides(rides) {
    const container = document.getElementById('activeRidesContainer');
    if (!container) return;
    
    if (rides.length === 0) {
        container.innerHTML = '<p style="color: #666; text-align: center;">No active rides at the moment</p>';
        return;
    }
    
    let html = '<div style="display: grid; gap: 15px;">';
    
    rides.forEach(ride => {
        const statusColor = {
            'PENDING': '#ffc107',
            'MATCHED': '#17a2b8',
            'ACCEPTED': '#28a745',
            'IN_PROGRESS': '#007bff',
            'COMPLETED': '#6c757d',
            'CANCELLED': '#dc3545'
        }[ride.status] || '#6c757d';
        
        html += `
            <div style="border: 1px solid #ddd; border-radius: 8px; padding: 15px; background: #f8f9fa;">
                <div style="display: flex; justify-content: space-between; align-items: start; margin-bottom: 10px;">
                    <div>
                        <strong style="font-size: 1.1em; color: #333;">${ride.rideId}</strong>
                        <span style="background: ${statusColor}; color: white; padding: 4px 8px; border-radius: 4px; font-size: 0.85em; margin-left: 10px;">
                            ${ride.status}
                        </span>
                    </div>
                </div>
                <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 10px; font-size: 0.9em;">
                    <div><strong>Rider:</strong> ${ride.riderId}</div>
                    <div><strong>Driver:</strong> ${ride.driverId || 'Not assigned'}</div>
                    <div><strong>Trip ID:</strong> ${ride.tripId || '-'}</div>
                    <div><strong>Created:</strong> ${ride.createdAt ? new Date(ride.createdAt).toLocaleString() : '-'}</div>
                    ${ride.matchedAt ? `<div><strong>Matched:</strong> ${new Date(ride.matchedAt).toLocaleString()}</div>` : ''}
                    ${ride.acceptedAt ? `<div><strong>Accepted:</strong> ${new Date(ride.acceptedAt).toLocaleString()}</div>` : ''}
                </div>
            </div>
        `;
    });
    
    html += '</div>';
    container.innerHTML = html;
}

// Auto-refresh active rides every 3 seconds
let activeRidesInterval = null;

function startActiveRidesPolling() {
    // Fetch immediately
    fetchActiveRides();
    
    // Then fetch every 3 seconds
    activeRidesInterval = setInterval(() => {
        fetchActiveRides();
    }, 3000);
}

// Initialize active rides polling when page loads
document.addEventListener('DOMContentLoaded', () => {
    if (document.getElementById('activeRidesContainer')) {
        startActiveRidesPolling();
    }
});

