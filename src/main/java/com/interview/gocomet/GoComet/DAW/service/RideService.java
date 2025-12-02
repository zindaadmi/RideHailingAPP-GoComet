package com.interview.gocomet.GoComet.DAW.service;

import com.interview.gocomet.GoComet.DAW.dto.RideRequest;
import com.interview.gocomet.GoComet.DAW.dto.RideResponse;
import com.interview.gocomet.GoComet.DAW.model.Driver;
import com.interview.gocomet.GoComet.DAW.model.Ride;
import com.interview.gocomet.GoComet.DAW.model.RideStatus;
import com.interview.gocomet.GoComet.DAW.repository.RideRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RideService {
    
    private final RideRepository rideRepository;
    private final DriverMatchingService driverMatchingService;
    
    /**
     * Create a new ride request with idempotency support
     */
    @Transactional
    @CacheEvict(value = "rides", key = "#result.rideId")
    public RideResponse createRide(RideRequest request) {
        // Check idempotency
        if (request.getIdempotencyKey() != null) {
            Optional<Ride> existingRide = rideRepository.findByIdempotencyKey(request.getIdempotencyKey());
            if (existingRide.isPresent()) {
                log.info("Idempotent ride request, returning existing ride: {}", existingRide.get().getRideId());
                return mapToResponse(existingRide.get());
            }
        }
        
        // Create new ride
        Ride ride = Ride.builder()
            .rideId("RIDE-" + UUID.randomUUID().toString())
            .riderId(request.getRiderId())
            .pickupLatitude(request.getPickupLatitude())
            .pickupLongitude(request.getPickupLongitude())
            .destinationLatitude(request.getDestinationLatitude())
            .destinationLongitude(request.getDestinationLongitude())
            .pickupAddress(request.getPickupAddress())
            .destinationAddress(request.getDestinationAddress())
            .tier(request.getTier())
            .paymentMethod(request.getPaymentMethod())
            .status(RideStatus.PENDING)
            .idempotencyKey(request.getIdempotencyKey())
            .build();
        
        ride = rideRepository.save(ride);
        
        // Attempt to match driver asynchronously (for now, synchronous)
        try {
            Driver matchedDriver = driverMatchingService.matchDriver(
                request.getPickupLatitude(),
                request.getPickupLongitude()
            );
            
            if (matchedDriver != null) {
                ride.setDriverId(matchedDriver.getId());
                ride.setStatus(RideStatus.MATCHED);
                ride.setMatchedAt(LocalDateTime.now());
                ride = rideRepository.save(ride);
                log.info("Ride {} matched with driver {}", ride.getRideId(), matchedDriver.getDriverId());
            } else {
                log.warn("No driver available for ride {}", ride.getRideId());
            }
        } catch (Exception e) {
            log.error("Error matching driver for ride {}: {}", ride.getRideId(), e.getMessage(), e);
        }
        
        return mapToResponse(ride);
    }
    
    /**
     * Get ride status with caching
     */
    @Cacheable(value = "rides", key = "#rideId")
    @Transactional(readOnly = true)
    public RideResponse getRide(String rideId) {
        Ride ride = rideRepository.findByRideId(rideId)
            .orElseThrow(() -> new RuntimeException("Ride not found: " + rideId));
        
        return mapToResponse(ride);
    }
    
    /**
     * Update ride status when driver accepts
     */
    @Transactional
    @CacheEvict(value = "rides", key = "#rideId")
    public void updateRideStatus(String rideId, RideStatus status) {
        Ride ride = rideRepository.findByRideId(rideId)
            .orElseThrow(() -> new RuntimeException("Ride not found: " + rideId));
        
        ride.setStatus(status);
        if (status == RideStatus.ACCEPTED) {
            ride.setAcceptedAt(LocalDateTime.now());
        }
        
        rideRepository.save(ride);
    }
    
    /**
     * Link trip to ride
     */
    @Transactional
    @CacheEvict(value = "rides", key = "#rideId")
    public void linkTrip(String rideId, Long tripId) {
        Ride ride = rideRepository.findByRideId(rideId)
            .orElseThrow(() -> new RuntimeException("Ride not found: " + rideId));
        
        ride.setTripId(tripId);
        ride.setStatus(RideStatus.IN_PROGRESS);
        rideRepository.save(ride);
    }
    
    /**
     * Get ride by ID (for internal use)
     */
    @Transactional(readOnly = true)
    public Ride getRideById(Long rideId) {
        return rideRepository.findById(rideId)
            .orElseThrow(() -> new RuntimeException("Ride not found: " + rideId));
    }
    
    private RideResponse mapToResponse(Ride ride) {
        return RideResponse.builder()
            .rideId(ride.getRideId())
            .riderId(ride.getRiderId())
            .status(ride.getStatus())
            .driverId(ride.getDriverId())
            .tripId(ride.getTripId())
            .createdAt(ride.getCreatedAt())
            .matchedAt(ride.getMatchedAt())
            .acceptedAt(ride.getAcceptedAt())
            .build();
    }
}

