package com.interview.gocomet.GoComet.DAW.controller;

import com.interview.gocomet.GoComet.DAW.dto.LocationUpdateRequest;
import com.interview.gocomet.GoComet.DAW.model.Driver;
import com.interview.gocomet.GoComet.DAW.service.DriverService;
import com.interview.gocomet.GoComet.DAW.service.RideService;
import com.interview.gocomet.GoComet.DAW.service.TripService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/v1/drivers")
@RequiredArgsConstructor
@Slf4j
public class DriverController {
    
    private final DriverService driverService;
    private final RideService rideService;
    private final TripService tripService;
    
    @PostMapping("/{id}/location")
    public ResponseEntity<Driver> updateLocation(
            @PathVariable String id,
            @Valid @RequestBody LocationUpdateRequest request) {
        log.debug("Updating location for driver: {}", id);
        try {
            Driver driver = driverService.updateLocation(
                id,
                request.getLatitude(),
                request.getLongitude()
            );
            return ResponseEntity.ok(driver);
        } catch (RuntimeException e) {
            log.error("Error updating driver location: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            log.error("Error updating driver location: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @PostMapping("/{id}/accept")
    public ResponseEntity<?> acceptRide(
            @PathVariable String id,
            @RequestParam(required = false) Long rideId,
            @RequestParam(required = false) String rideIdString) {
        log.info("Driver {} accepting ride (numeric: {}, string: {})", id, rideId, rideIdString);
        
        try {
            com.interview.gocomet.GoComet.DAW.model.Ride ride = null;
            Long numericRideId = null;
            
            // Support both numeric ID and alphanumeric rideId string
            if (rideIdString != null && !rideIdString.isEmpty()) {
                // Look up by alphanumeric rideId string
                try {
                    ride = rideService.getRideEntityByRideId(rideIdString);
                    numericRideId = ride.getId();
                } catch (RuntimeException e) {
                    Map<String, String> error = new HashMap<>();
                    error.put("error", "Ride not found: " + rideIdString);
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
                }
            } else if (rideId != null) {
                // Use numeric ID directly
                numericRideId = rideId;
                ride = rideService.getRideById(rideId);
            } else {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Either rideId (numeric) or rideIdString (alphanumeric) must be provided");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }
            
            if (ride == null) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Ride not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
            }
            
            // Verify the ride is matched to this driver
            // Get driver by driverId string to get numeric ID
            var driverOpt = driverService.getDriver(id);
            if (driverOpt.isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Driver not found: " + id);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
            }
            
            var driver = driverOpt.get();
            if (ride.getDriverId() == null || !ride.getDriverId().equals(driver.getId())) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "This ride is not matched to driver " + id);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }
            
            // Accept the ride
            driverService.acceptRide(id, numericRideId);
            
            // Update ride status
            rideService.updateRideStatus(ride.getRideId(), 
                com.interview.gocomet.GoComet.DAW.model.RideStatus.ACCEPTED);
            
            // Automatically start trip when driver accepts
            try {
                tripService.startTrip(numericRideId);
            } catch (Exception e) {
                log.warn("Could not start trip automatically: {}", e.getMessage());
            }
            
            return ResponseEntity.ok().build();
        } catch (IllegalStateException e) {
            log.error("Error accepting ride: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (RuntimeException e) {
            log.error("Error accepting ride: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        } catch (Exception e) {
            log.error("Error accepting ride: {}", e.getMessage(), e);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage() != null ? e.getMessage() : "Failed to accept ride");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<?> getDriver(@PathVariable String id) {
        log.info("Getting driver: {}", id);
        try {
            var driver = driverService.getDriver(id);
            if (driver.isPresent()) {
                return ResponseEntity.ok(driver.get());
            } else {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Driver not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
            }
        } catch (Exception e) {
            log.error("Error getting driver: {}", e.getMessage(), e);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage() != null ? e.getMessage() : "Failed to get driver");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}

