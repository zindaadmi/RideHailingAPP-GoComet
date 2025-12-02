package com.interview.gocomet.GoComet.DAW.controller;

import com.interview.gocomet.GoComet.DAW.model.Trip;
import com.interview.gocomet.GoComet.DAW.service.TripService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/v1/trips")
@RequiredArgsConstructor
@Slf4j
public class TripController {
    
    private final TripService tripService;
    
    @GetMapping("/{id}")
    public ResponseEntity<?> getTrip(@PathVariable Long id) {
        log.info("Getting trip: {}", id);
        try {
            Trip trip = tripService.getTrip(id);
            return ResponseEntity.ok(trip);
        } catch (RuntimeException e) {
            log.error("Error getting trip: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage() != null ? e.getMessage() : "Trip not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        } catch (Exception e) {
            log.error("Error getting trip: {}", e.getMessage(), e);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage() != null ? e.getMessage() : "Failed to get trip");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    @PostMapping("/{id}/start")
    public ResponseEntity<?> startTrip(@PathVariable Long id) {
        log.info("Starting trip for ride: {}", id);
        try {
            Trip trip = tripService.startTrip(id);
            return ResponseEntity.status(HttpStatus.CREATED).body(trip);
        } catch (IllegalStateException e) {
            log.error("Error starting trip: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage() != null ? e.getMessage() : "Invalid ride state");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (RuntimeException e) {
            log.error("Error starting trip: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage() != null ? e.getMessage() : "Ride not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        } catch (Exception e) {
            log.error("Error starting trip: {}", e.getMessage(), e);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage() != null ? e.getMessage() : "Failed to start trip");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    @PostMapping("/{id}/end")
    public ResponseEntity<?> endTrip(
            @PathVariable Long id,
            @RequestParam(required = false) Double endLatitude,
            @RequestParam(required = false) Double endLongitude) {
        log.info("Ending trip: {}", id);
        try {
            Trip trip = tripService.endTrip(id, endLatitude, endLongitude);
            return ResponseEntity.ok(trip);
        } catch (IllegalStateException e) {
            log.error("Error ending trip: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage() != null ? e.getMessage() : "Invalid trip state");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (RuntimeException e) {
            log.error("Error ending trip: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage() != null ? e.getMessage() : "Trip not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        } catch (Exception e) {
            log.error("Error ending trip: {}", e.getMessage(), e);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage() != null ? e.getMessage() : "Failed to end trip");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    @PostMapping("/{id}/pause")
    public ResponseEntity<?> pauseTrip(@PathVariable Long id) {
        log.info("Pausing trip: {}", id);
        try {
            Trip trip = tripService.pauseTrip(id);
            return ResponseEntity.ok(trip);
        } catch (IllegalStateException e) {
            log.error("Error pausing trip: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage() != null ? e.getMessage() : "Invalid trip state");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (RuntimeException e) {
            log.error("Error pausing trip: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage() != null ? e.getMessage() : "Trip not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        } catch (Exception e) {
            log.error("Error pausing trip: {}", e.getMessage(), e);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage() != null ? e.getMessage() : "Failed to pause trip");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    @PostMapping("/{id}/resume")
    public ResponseEntity<?> resumeTrip(@PathVariable Long id) {
        log.info("Resuming trip: {}", id);
        try {
            Trip trip = tripService.resumeTrip(id);
            return ResponseEntity.ok(trip);
        } catch (IllegalStateException e) {
            log.error("Error resuming trip: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage() != null ? e.getMessage() : "Invalid trip state");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (RuntimeException e) {
            log.error("Error resuming trip: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage() != null ? e.getMessage() : "Trip not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        } catch (Exception e) {
            log.error("Error resuming trip: {}", e.getMessage(), e);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage() != null ? e.getMessage() : "Failed to resume trip");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}

