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
     */
    @Transactional
    @CacheEvict(value = "availableDrivers", allEntries = true)
    public Driver matchDriver(Double latitude, Double longitude) {
        List<Driver> candidates = findAvailableDrivers(latitude, longitude);
        
        if (candidates.isEmpty()) {
            log.warn("No available drivers found near location: {}, {}", latitude, longitude);
            return null;
        }
        
        // Try to assign the closest available driver
        for (Driver driver : candidates) {
            try {
                // Use optimistic locking - check if driver is still available
                Driver currentDriver = driverRepository.findById(driver.getId())
                    .orElse(null);
                
                if (currentDriver != null && currentDriver.getStatus() == DriverStatus.AVAILABLE) {
                    currentDriver.setStatus(DriverStatus.ASSIGNED);
                    currentDriver = driverRepository.save(currentDriver);
                    log.info("Matched driver {} to ride request", currentDriver.getDriverId());
                    return currentDriver;
                }
            } catch (Exception e) {
                log.warn("Failed to assign driver {}: {}", driver.getDriverId(), e.getMessage());
                // Continue to next candidate
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

