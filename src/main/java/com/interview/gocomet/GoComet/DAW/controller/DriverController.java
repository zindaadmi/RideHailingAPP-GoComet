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
    public ResponseEntity<Void> acceptRide(
            @PathVariable String id,
            @RequestParam Long rideId) {
        log.info("Driver {} accepting ride {}", id, rideId);
        try {
            driverService.acceptRide(id, rideId);
            // Find ride by ID and update status
            com.interview.gocomet.GoComet.DAW.model.Ride ride = rideService.getRideById(rideId);
            if (ride != null) {
                rideService.updateRideStatus(ride.getRideId(), 
                    com.interview.gocomet.GoComet.DAW.model.RideStatus.ACCEPTED);
                // Automatically start trip when driver accepts
                try {
                    tripService.startTrip(rideId);
                } catch (Exception e) {
                    log.warn("Could not start trip automatically: {}", e.getMessage());
                }
            }
            return ResponseEntity.ok().build();
        } catch (IllegalStateException e) {
            log.error("Error accepting ride: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (RuntimeException e) {
            log.error("Error accepting ride: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            log.error("Error accepting ride: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
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

