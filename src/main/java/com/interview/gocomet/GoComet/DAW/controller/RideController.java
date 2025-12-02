package com.interview.gocomet.GoComet.DAW.controller;

import com.interview.gocomet.GoComet.DAW.dto.RideRequest;
import com.interview.gocomet.GoComet.DAW.dto.RideResponse;
import com.interview.gocomet.GoComet.DAW.service.RideService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/v1/rides")
@RequiredArgsConstructor
@Slf4j
public class RideController {
    
    private final RideService rideService;
    
    @PostMapping
    public ResponseEntity<?> createRide(@Valid @RequestBody RideRequest request) {
        log.info("Creating ride request for rider: {}", request.getRiderId());
        try {
            RideResponse response = rideService.createRide(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            log.error("Error creating ride: {}", e.getMessage(), e);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage() != null ? e.getMessage() : "Failed to create ride");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<?> getRide(@PathVariable String id) {
        log.info("Getting ride status: {}", id);
        try {
            RideResponse response = rideService.getRide(id);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Error getting ride: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage() != null ? e.getMessage() : "Ride not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        } catch (Exception e) {
            log.error("Error getting ride: {}", e.getMessage(), e);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage() != null ? e.getMessage() : "Failed to get ride");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}

