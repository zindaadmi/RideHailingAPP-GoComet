package com.interview.gocomet.GoComet.DAW.service;

import com.interview.gocomet.GoComet.DAW.model.Ride;
import com.interview.gocomet.GoComet.DAW.model.RideStatus;
import com.interview.gocomet.GoComet.DAW.model.Trip;
import com.interview.gocomet.GoComet.DAW.model.TripStatus;
import com.interview.gocomet.GoComet.DAW.repository.RideRepository;
import com.interview.gocomet.GoComet.DAW.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TripService {
    
    private static final double BASE_FARE = 25.0;
    private static final double PER_KM_RATE = 8.0;
    private static final double PER_MINUTE_RATE = 1.0;
    private static final double MIN_FARE = 40.0;
    
    private final TripRepository tripRepository;
    private final RideRepository rideRepository;
    private final DriverService driverService;
    private final RideService rideService;
    
    /**
     * Start a trip
     */
    @Transactional
    public Trip startTrip(Long rideId) {
        Ride ride = rideRepository.findById(rideId)
            .orElseThrow(() -> new RuntimeException("Ride not found: " + rideId));
        
        if (ride.getDriverId() == null) {
            throw new IllegalStateException("Ride has no assigned driver");
        }
        
        Trip trip = Trip.builder()
            .tripId("TRIP-" + UUID.randomUUID().toString())
            .rideId(rideId)
            .driverId(ride.getDriverId())
            .riderId(ride.getRiderId())
            .status(TripStatus.STARTED)
            .startLatitude(ride.getPickupLatitude())
            .startLongitude(ride.getPickupLongitude())
            .endLatitude(ride.getDestinationLatitude())
            .endLongitude(ride.getDestinationLongitude())
            .startTime(LocalDateTime.now())
            .surgeMultiplier(1.0)
            .build();
        
        trip = tripRepository.save(trip);
        rideService.linkTrip(ride.getRideId(), trip.getId());
        
        log.info("Started trip {} for ride {}", trip.getTripId(), rideId);
        return trip;
    }
    
    /**
     * End trip and calculate fare
     */
    @Transactional
    public Trip endTrip(Long tripId, Double endLatitude, Double endLongitude) {
        Trip trip = tripRepository.findById(tripId)
            .orElseThrow(() -> new RuntimeException("Trip not found: " + tripId));
        
        if (trip.getStatus() == TripStatus.COMPLETED) {
            throw new IllegalStateException("Trip already completed");
        }
        
        trip.setEndTime(LocalDateTime.now());
        trip.setEndLatitude(endLatitude != null ? endLatitude : trip.getEndLatitude());
        trip.setEndLongitude(endLongitude != null ? endLongitude : trip.getEndLongitude());
        
        // Calculate distance and duration
        double distance = calculateDistance(
            trip.getStartLatitude(), trip.getStartLongitude(),
            trip.getEndLatitude(), trip.getEndLongitude()
        );
        trip.setDistanceKm(distance);
        
        Duration duration = Duration.between(trip.getStartTime(), trip.getEndTime());
        long totalSeconds = duration.getSeconds();
        if (trip.getTotalPauseDurationSeconds() != null) {
            totalSeconds -= trip.getTotalPauseDurationSeconds();
        }
        double durationMinutes = totalSeconds / 60.0;
        trip.setDurationMinutes(durationMinutes);
        
        // Calculate fare
        calculateFare(trip);
        
        trip.setStatus(TripStatus.COMPLETED);
        trip = tripRepository.save(trip);
        
        // Update ride status to COMPLETED
        Ride ride = rideRepository.findById(trip.getRideId())
            .orElse(null);
        if (ride != null) {
            ride.setStatus(RideStatus.COMPLETED);
            rideRepository.save(ride);
            log.info("Updated ride {} status to COMPLETED", ride.getRideId());
        }
        
        // Release driver
        driverService.releaseDriver(trip.getDriverId());
        
        log.info("Completed trip {} with fare: {}", trip.getTripId(), trip.getTotalFare());
        return trip;
    }
    
    /**
     * Pause trip
     */
    @Transactional
    public Trip pauseTrip(Long tripId) {
        Trip trip = tripRepository.findById(tripId)
            .orElseThrow(() -> new RuntimeException("Trip not found: " + tripId));
        
        if (trip.getStatus() != TripStatus.STARTED && trip.getStatus() != TripStatus.RESUMED) {
            throw new IllegalStateException("Trip cannot be paused in current status");
        }
        
        trip.setStatus(TripStatus.PAUSED);
        trip.setPauseStartTime(LocalDateTime.now());
        trip = tripRepository.save(trip);
        
        log.info("Paused trip {}", tripId);
        return trip;
    }
    
    /**
     * Resume trip
     */
    @Transactional
    public Trip resumeTrip(Long tripId) {
        Trip trip = tripRepository.findById(tripId)
            .orElseThrow(() -> new RuntimeException("Trip not found: " + tripId));
        
        if (trip.getStatus() != TripStatus.PAUSED) {
            throw new IllegalStateException("Trip is not paused");
        }
        
        if (trip.getPauseStartTime() != null) {
            Duration pauseDuration = Duration.between(trip.getPauseStartTime(), LocalDateTime.now());
            long pauseSeconds = pauseDuration.getSeconds();
            trip.setTotalPauseDurationSeconds(
                (trip.getTotalPauseDurationSeconds() != null ? trip.getTotalPauseDurationSeconds() : 0) + pauseSeconds
            );
        }
        
        trip.setStatus(TripStatus.RESUMED);
        trip.setPauseStartTime(null);
        trip = tripRepository.save(trip);
        
        log.info("Resumed trip {}", tripId);
        return trip;
    }
    
    /**
     * Calculate fare based on distance, time, and surge
     */
    private void calculateFare(Trip trip) {
        double baseFare = BASE_FARE;
        double distanceFare = trip.getDistanceKm() * PER_KM_RATE;
        double timeFare = trip.getDurationMinutes() * PER_MINUTE_RATE;
        
        trip.setBaseFare(baseFare);
        trip.setDistanceFare(distanceFare);
        trip.setTimeFare(timeFare);
        
        double totalFare = (baseFare + distanceFare + timeFare) * trip.getSurgeMultiplier();
        trip.setTotalFare(Math.max(totalFare, MIN_FARE));
    }
    
    /**
     * Calculate distance using Haversine formula
     */
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final double EARTH_RADIUS_KM = 6371.0;
        
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return EARTH_RADIUS_KM * c;
    }
    
    public Trip getTrip(Long tripId) {
        return tripRepository.findById(tripId)
            .orElseThrow(() -> new RuntimeException("Trip not found: " + tripId));
    }
}

