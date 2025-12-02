package com.interview.gocomet.GoComet.DAW.service;

import com.interview.gocomet.GoComet.DAW.model.Driver;
import com.interview.gocomet.GoComet.DAW.model.DriverStatus;
import com.interview.gocomet.GoComet.DAW.repository.DriverRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DriverMatchingService {
    
    private static final double SEARCH_RADIUS_KM = 10.0;
    private static final double EARTH_RADIUS_KM = 6371.0;
    private static final int MAX_DRIVERS_TO_CHECK = 50;
    
    // Static counter for round-robin driver selection
    private static volatile int driverRotationCounter = 0;
    
    private final DriverRepository driverRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    
    /**
     * Find the best available driver near the pickup location
     * Uses caching for frequently accessed locations
     */
    @Cacheable(value = "availableDrivers", key = "#latitude + '_' + #longitude", unless = "#result.isEmpty()")
    @Transactional(readOnly = true)
    public List<Driver> findAvailableDrivers(Double latitude, Double longitude) {
        // Calculate bounding box for efficient query
        double[] bounds = calculateBoundingBox(latitude, longitude, SEARCH_RADIUS_KM);
        
        List<Driver> drivers = driverRepository.findNearbyAvailableDrivers(
            latitude, longitude,
            bounds[0], bounds[1], // minLat, maxLat
            bounds[2], bounds[3], // minLng, maxLng
            MAX_DRIVERS_TO_CHECK
        );
        
        // Sort by distance and return top candidates
        return drivers.stream()
            .sorted((d1, d2) -> {
                double dist1 = calculateDistance(latitude, longitude, d1.getLatitude(), d1.getLongitude());
                double dist2 = calculateDistance(latitude, longitude, d2.getLatitude(), d2.getLongitude());
                return Double.compare(dist1, dist2);
            })
            .limit(10)
            .collect(Collectors.toList());
    }
    
    /**
     * Match a driver to a ride request
     * Uses optimistic locking to prevent race conditions
     * Implements round-robin selection for fair driver distribution
     */
    @Transactional
    @CacheEvict(value = "availableDrivers", allEntries = true)
    public Driver matchDriver(Double latitude, Double longitude) {
        List<Driver> candidates = findAvailableDrivers(latitude, longitude);
        
        if (candidates.isEmpty()) {
            log.warn("No available drivers found near location: {}, {}", latitude, longitude);
            return null;
        }
        
        // Sort candidates by driverId for consistent ordering
        candidates.sort((d1, d2) -> d1.getDriverId().compareTo(d2.getDriverId()));
        
        // Get all available drivers for round-robin
        List<Driver> allAvailable = driverRepository.findByStatus(DriverStatus.AVAILABLE);
        allAvailable.sort((d1, d2) -> d1.getDriverId().compareTo(d2.getDriverId()));
        
        // Filter candidates to only include those that are actually available
        List<Driver> availableCandidates = candidates.stream()
            .filter(c -> allAvailable.stream()
                .anyMatch(a -> a.getId().equals(c.getId())))
            .collect(Collectors.toList());
        
        if (availableCandidates.isEmpty()) {
            availableCandidates = candidates; // Fallback to all candidates
        }
        
        // Round-robin: Use counter to select different driver each time
        Driver selectedDriver = null;
        if (!availableCandidates.isEmpty()) {
            int index = driverRotationCounter % availableCandidates.size();
            selectedDriver = availableCandidates.get(index);
            driverRotationCounter++; // Increment for next time
            log.debug("Round-robin selection: index {} of {} candidates", index, availableCandidates.size());
        }
        
        // Try to assign the selected driver
        if (selectedDriver != null) {
            try {
                // Use optimistic locking - check if driver is still available
                Driver currentDriver = driverRepository.findById(selectedDriver.getId())
                    .orElse(null);
                
                if (currentDriver != null && currentDriver.getStatus() == DriverStatus.AVAILABLE) {
                    currentDriver.setStatus(DriverStatus.ASSIGNED);
                    currentDriver = driverRepository.save(currentDriver);
                    log.info("Matched driver {} to ride request (round-robin: {}/{})", 
                        currentDriver.getDriverId(), 
                        allAvailable.indexOf(selectedDriver) + 1, 
                        allAvailable.size());
                    return currentDriver;
                }
            } catch (Exception e) {
                log.warn("Failed to assign driver {}: {}", selectedDriver.getDriverId(), e.getMessage());
            }
        }
        
        // Fallback: try all candidates in order
        for (Driver driver : candidates) {
            try {
                Driver currentDriver = driverRepository.findById(driver.getId())
                    .orElse(null);
                
                if (currentDriver != null && currentDriver.getStatus() == DriverStatus.AVAILABLE) {
                    currentDriver.setStatus(DriverStatus.ASSIGNED);
                    currentDriver = driverRepository.save(currentDriver);
                    log.info("Matched driver {} to ride request (fallback)", currentDriver.getDriverId());
                    return currentDriver;
                }
            } catch (Exception e) {
                log.warn("Failed to assign driver {}: {}", driver.getDriverId(), e.getMessage());
            }
        }
        
        log.warn("All candidate drivers were already assigned");
        return null;
    }
    
    /**
     * Calculate distance between two points using Haversine formula
     */
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return EARTH_RADIUS_KM * c;
    }
    
    /**
     * Calculate bounding box for efficient spatial query
     */
    private double[] calculateBoundingBox(double latitude, double longitude, double radiusKm) {
        double latDelta = radiusKm / EARTH_RADIUS_KM * (180.0 / Math.PI);
        double lonDelta = radiusKm / (EARTH_RADIUS_KM * Math.cos(Math.toRadians(latitude))) * (180.0 / Math.PI);
        
        return new double[]{
            latitude - latDelta,  // minLat
            latitude + latDelta,  // maxLat
            longitude - lonDelta, // minLng
            longitude + lonDelta  // maxLng
        };
    }
    
    /**
     * Invalidate cache when driver status changes
     */
    @CacheEvict(value = "availableDrivers", allEntries = true)
    public void invalidateDriverCache() {
        log.debug("Invalidated available drivers cache");
    }
}

