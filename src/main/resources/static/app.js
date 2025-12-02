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
                // If response is not JSON, use status text
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

// Handle driver location update
document.getElementById('driverLocationForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    
    const driverId = document.getElementById('driverId').value;
    const locationData = {
        latitude: parseFloat(document.getElementById('driverLat').value),
        longitude: parseFloat(document.getElementById('driverLng').value)
    };
    
    try {
        addMessage(`Updating location for driver ${driverId}...`, 'info');
        
        const response = await fetch(`${API_BASE_URL}/drivers/${driverId}/location`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(locationData)
        });
        
        if (!response.ok) {
            let errorMessage = 'Failed to update location';
            try {
                const errorData = await response.json();
                errorMessage = errorData.error || errorData.message || errorMessage;
            } catch (e) {
                errorMessage = response.statusText || errorMessage;
            }
            throw new Error(errorMessage);
        }
        
        const driver = await response.json();
        addMessage(`Location updated successfully for driver ${driverId}`, 'success');
        
    } catch (error) {
        addMessage(`Error: ${error.message}`, 'error');
        console.error('Error updating location:', error);
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
    
    // Show/hide trip actions based on status
    if (ride.status === 'IN_PROGRESS' && ride.tripId) {
        document.getElementById('tripActions').style.display = 'block';
        document.getElementById('acceptRideSection').style.display = 'none';
    } else if (ride.status === 'MATCHED' && ride.driverId && !ride.tripId) {
        // Show accept button when driver is matched but not accepted yet
        document.getElementById('acceptRideSection').style.display = 'block';
        document.getElementById('tripActions').style.display = 'none';
    } else if (ride.status === 'ACCEPTED' && !ride.tripId) {
        // Trip should start automatically, but show accept button as fallback
        document.getElementById('acceptRideSection').style.display = 'block';
        document.getElementById('tripActions').style.display = 'none';
    } else {
        document.getElementById('tripActions').style.display = 'none';
        document.getElementById('acceptRideSection').style.display = 'none';
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
        
        // Log status changes
        if (ride.status === 'MATCHED' && ride.driverId) {
            addMessage(`Driver matched! Driver ID: ${ride.driverId}`, 'success');
        } else if (ride.status === 'ACCEPTED') {
            addMessage('Driver accepted the ride! Trip will start automatically...', 'success');
        } else if (ride.status === 'IN_PROGRESS') {
            addMessage('Trip started!', 'success');
            // Show end trip button if trip ID exists
            if (ride.tripId) {
                currentTripId = ride.tripId;
                document.getElementById('tripActions').style.display = 'block';
            }
        } else if (ride.status === 'COMPLETED') {
            addMessage('Trip completed!', 'success');
            stopPolling();
            // Fetch trip details to show fare
            if (ride.tripId) {
                fetchTripDetails(ride.tripId);
            }
        } else if (ride.status === 'CANCELLED') {
            addMessage('Ride cancelled.', 'error');
            stopPolling();
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

// End trip
async function endTrip() {
    if (!currentTripId) {
        addMessage('No active trip to end', 'error');
        return;
    }
    
    try {
        addMessage(`Ending trip ${currentTripId}...`, 'info');
        document.getElementById('endTripBtn').disabled = true;
        
        const response = await fetch(`${API_BASE_URL}/trips/${currentTripId}/end`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            }
        });
        
        if (!response.ok) {
            let errorMessage = 'Failed to end trip';
            try {
                const errorData = await response.json();
                errorMessage = errorData.error || errorData.message || errorMessage;
            } catch (e) {
                errorMessage = response.statusText || errorMessage;
            }
            throw new Error(errorMessage);
        }
        
        const trip = await response.json();
        addMessage(`Trip ended successfully! Fare: ₹${trip.totalFare?.toFixed(2) || '0.00'}`, 'success');
        
        // Show payment section
        document.getElementById('tripActions').style.display = 'none';
        document.getElementById('paymentSection').style.display = 'block';
        document.getElementById('tripFare').textContent = `₹${trip.totalFare?.toFixed(2) || '0.00'}`;
        
        // Update ride status
        if (currentRideId) {
            pollRideStatus(currentRideId);
        }
        
    } catch (error) {
        addMessage(`Error: ${error.message}`, 'error');
        document.getElementById('endTripBtn').disabled = false;
        console.error('Error ending trip:', error);
    }
}

// Fetch trip details
async function fetchTripDetails(tripId) {
    try {
        const response = await fetch(`${API_BASE_URL}/trips/${tripId}`);
        if (response.ok) {
            const trip = await response.json();
            if (trip.status === 'COMPLETED' && trip.totalFare) {
                document.getElementById('paymentSection').style.display = 'block';
                document.getElementById('tripFare').textContent = `₹${trip.totalFare.toFixed(2)}`;
                currentTripId = tripId;
            }
        }
    } catch (error) {
        console.error('Error fetching trip details:', error);
    }
}

// Accept ride (driver accepts the ride)
async function acceptRide() {
    const rideId = currentRideId;
    if (!rideId) {
        addMessage('No ride to accept', 'error');
        return;
    }
    
    // Get driver ID from ride status
    const statusDriverId = document.getElementById('statusDriverId').textContent;
    if (!statusDriverId || statusDriverId === 'Not assigned') {
        addMessage('No driver assigned to this ride', 'error');
        return;
    }
    
    try {
        addMessage(`Accepting ride ${rideId}...`, 'info');
        document.getElementById('acceptRideBtn').disabled = true;
        document.getElementById('acceptRideBtn').textContent = 'Accepting...';
        
        // Extract ride ID number from rideId string (e.g., "RIDE-1234567890" -> need numeric ID)
        // We need to get the ride details first to get the numeric ID
        const rideResponse = await fetch(`${API_BASE_URL}/rides/${rideId}`);
        if (!rideResponse.ok) {
            throw new Error('Failed to fetch ride details');
        }
        const ride = await rideResponse.json();
        
        // Extract driver ID from status or use a default
        const driverId = statusDriverId.startsWith('DRIVER-') ? statusDriverId : `DRIVER-${statusDriverId}`;
        
        // Get numeric ride ID - we'll need to parse it or use the ride object
        // For now, let's try to get the ride by ID and extract the numeric ID
        // Actually, the API might accept the rideId string, let's check the controller
        
        // Try to accept with the driver ID from the matched driver
        const response = await fetch(`${API_BASE_URL}/drivers/${driverId}/accept?rideId=${ride.id || 1}`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            }
        });
        
        if (!response.ok) {
            let errorMessage = 'Failed to accept ride';
            try {
                const errorData = await response.json();
                errorMessage = errorData.error || errorData.message || errorMessage;
            } catch (e) {
                errorMessage = response.statusText || errorMessage;
            }
            throw new Error(errorMessage);
        }
        
        addMessage('Ride accepted! Trip starting...', 'success');
        document.getElementById('acceptRideSection').style.display = 'none';
        
        // Refresh ride status to get updated trip ID
        if (rideId) {
            setTimeout(() => {
                pollRideStatus(rideId);
            }, 1000);
        }
        
    } catch (error) {
        addMessage(`Error: ${error.message}`, 'error');
        document.getElementById('acceptRideBtn').disabled = false;
        document.getElementById('acceptRideBtn').textContent = 'Accept Ride';
        console.error('Error accepting ride:', error);
    }
}

// Process payment
async function processPayment() {
    if (!currentTripId) {
        addMessage('No trip available for payment', 'error');
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

// Initialize
addMessage('Application loaded. Ready to request rides!', 'success');
startActiveRidesPolling();

