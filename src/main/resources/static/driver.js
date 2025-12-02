const API_BASE_URL = 'http://localhost:8080/v1';

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
        
        // Update driver status display
        updateDriverStatus(driver);
        
    } catch (error) {
        addMessage(`Error: ${error.message}`, 'error');
        console.error('Error updating location:', error);
    }
});

// Handle accept ride form
document.getElementById('acceptRideForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    
    const driverId = document.getElementById('acceptDriverId').value;
    const rideIdString = document.getElementById('acceptRideId').value.trim();
    
    if (!rideIdString) {
        addMessage('Please enter a valid ride ID', 'error');
        return;
    }
    
    try {
        addMessage(`Driver ${driverId} accepting ride ${rideIdString}...`, 'info');
        
        // Use rideIdString parameter for alphanumeric ride IDs
        const response = await fetch(`${API_BASE_URL}/drivers/${driverId}/accept?rideIdString=${encodeURIComponent(rideIdString)}`, {
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
        
        addMessage(`Ride ${rideIdString} accepted successfully! Trip will start automatically.`, 'success');
        
        // Clear the form
        document.getElementById('acceptRideId').value = '';
        
        // Fetch driver status to update display
        fetchDriverStatus(driverId);
        
        // Start polling for trip status using the ride ID
        setTimeout(() => {
            startPollingTripStatus(rideIdString);
        }, 2000);
        
    } catch (error) {
        addMessage(`Error: ${error.message}`, 'error');
        console.error('Error accepting ride:', error);
    }
});

// Update driver status display
function updateDriverStatus(driver) {
    document.getElementById('driverStatusId').textContent = driver.driverId || '-';
    document.getElementById('driverStatusStatus').textContent = driver.status || '-';
    document.getElementById('driverStatusStatus').className = `value status-badge ${driver.status || ''}`;
    document.getElementById('driverCurrentRideId').textContent = driver.currentRideId || 'None';
    document.getElementById('driverLocation').textContent = 
        `${driver.latitude?.toFixed(4) || '-'}, ${driver.longitude?.toFixed(4) || '-'}`;
    
    document.getElementById('driverStatusSection').style.display = 'block';
    
    // If driver is ON_TRIP, fetch trip details
    if (driver.status === 'ON_TRIP' && driver.currentRideId) {
        fetchTripByRideId(driver.currentRideId);
    }
}

// Fetch driver status
async function fetchDriverStatus(driverId) {
    try {
        const response = await fetch(`${API_BASE_URL}/drivers/${driverId}`);
        if (response.ok) {
            const driver = await response.json();
            updateDriverStatus(driver);
        }
    } catch (error) {
        console.error('Error fetching driver status:', error);
    }
}

// Fetch trip by ride ID
async function fetchTripByRideId(rideId) {
    try {
        // First get the ride to find the trip ID
        const rideResponse = await fetch(`${API_BASE_URL}/rides/${rideId}`);
        if (rideResponse.ok) {
            const ride = await rideResponse.json();
            if (ride.tripId) {
                currentTripId = ride.tripId;
                document.getElementById('driverCurrentTripId').textContent = ride.tripId || '-';
                
                // Check trip status
                const tripResponse = await fetch(`${API_BASE_URL}/trips/${ride.tripId}`);
                if (tripResponse.ok) {
                    const trip = await tripResponse.json();
                    if (trip.status === 'STARTED') {
                        document.getElementById('endTripSection').style.display = 'block';
                    } else if (trip.status === 'COMPLETED') {
                        document.getElementById('endTripSection').style.display = 'none';
                    }
                }
            }
        }
    } catch (error) {
        console.error('Error fetching trip:', error);
    }
}

// Poll trip status
async function pollTripStatus(rideId) {
    try {
        const response = await fetch(`${API_BASE_URL}/rides/${rideId}`);
        if (response.ok) {
            const ride = await response.json();
            if (ride.tripId) {
                currentTripId = ride.tripId;
                document.getElementById('driverCurrentTripId').textContent = ride.tripId || '-';
                
                if (ride.status === 'IN_PROGRESS' && ride.tripId) {
                    document.getElementById('endTripSection').style.display = 'block';
                } else if (ride.status === 'COMPLETED') {
                    document.getElementById('endTripSection').style.display = 'none';
                    stopPollingTripStatus();
                }
            }
        }
    } catch (error) {
        console.error('Error polling trip status:', error);
    }
}

// Start polling trip status
function startPollingTripStatus(rideId) {
    if (pollingInterval) {
        clearInterval(pollingInterval);
    }
    
    // Poll immediately
    pollTripStatus(rideId);
    
    // Then poll every 3 seconds
    pollingInterval = setInterval(() => {
        pollTripStatus(rideId);
    }, 3000);
}

// Stop polling trip status
function stopPollingTripStatus() {
    if (pollingInterval) {
        clearInterval(pollingInterval);
        pollingInterval = null;
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
        document.getElementById('endTripBtn').textContent = 'Ending...';
        
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
        addMessage('Rider can now make payment on the rider dashboard.', 'info');
        
        // Hide end trip button
        document.getElementById('endTripSection').style.display = 'none';
        document.getElementById('endTripBtn').disabled = false;
        document.getElementById('endTripBtn').textContent = 'End Trip';
        
        // Stop polling
        stopPollingTripStatus();
        
        // Update driver status
        const driverId = document.getElementById('acceptDriverId').value || document.getElementById('driverId').value;
        if (driverId) {
            fetchDriverStatus(driverId);
        }
        
    } catch (error) {
        addMessage(`Error: ${error.message}`, 'error');
        document.getElementById('endTripBtn').disabled = false;
        document.getElementById('endTripBtn').textContent = 'End Trip';
        console.error('Error ending trip:', error);
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
addMessage('Driver dashboard loaded. Ready to update location and accept rides!', 'success');

// Initialize active rides polling
if (document.getElementById('activeRidesContainer')) {
    startActiveRidesPolling();
}

