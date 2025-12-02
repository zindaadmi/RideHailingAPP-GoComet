package com.interview.gocomet.GoComet.DAW.service;

import com.interview.gocomet.GoComet.DAW.model.Driver;
import com.interview.gocomet.GoComet.DAW.model.DriverStatus;
import com.interview.gocomet.GoComet.DAW.repository.DriverRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DriverService {
    
    private final DriverRepository driverRepository;
    private final DriverMatchingService driverMatchingService;
    private final RedisTemplate<String, Object> redisTemplate;
    
    /**
     * Update driver location with caching for fast lookups
     */
    @Transactional
    @CacheEvict(value = "availableDrivers", allEntries = true)
    public Driver updateLocation(String driverId, Double latitude, Double longitude) {
        Driver driver = driverRepository.findByDriverId(driverId)
            .orElseThrow(() -> new RuntimeException("Driver not found: " + driverId));
        
        driver.setLatitude(latitude);
        driver.setLongitude(longitude);
        driver.setLastLocationUpdate(LocalDateTime.now());
        
        // Cache driver location for real-time queries (if Redis is available)
        try {
            String cacheKey = "driver:location:" + driverId;
            redisTemplate.opsForValue().set(cacheKey, driver, java.time.Duration.ofSeconds(5));
        } catch (Exception e) {
            log.debug("Redis not available, skipping cache update for driver: {}", driverId);
        }
        
        driver = driverRepository.save(driver);
        log.debug("Updated location for driver {}: {}, {}", driverId, latitude, longitude);
        
        return driver;
    }
    
    /**
     * Accept ride assignment
     */
    @Transactional
    @CacheEvict(value = "availableDrivers", allEntries = true)
    public Driver acceptRide(String driverId, Long rideId) {
        Driver driver = driverRepository.findByDriverId(driverId)
            .orElseThrow(() -> new RuntimeException("Driver not found: " + driverId));
        
        // Allow acceptance if driver is ASSIGNED or AVAILABLE (for flexibility)
        // If AVAILABLE, we'll set to ASSIGNED first, then ON_TRIP
        if (driver.getStatus() == DriverStatus.ON_TRIP) {
            throw new IllegalStateException("Driver is already on a trip");
        }
        
        if (driver.getStatus() == DriverStatus.OFFLINE) {
            throw new IllegalStateException("Driver is offline");
        }
        
        // If driver is AVAILABLE, set to ASSIGNED first (in case matching didn't set it)
        if (driver.getStatus() == DriverStatus.AVAILABLE) {
            log.warn("Driver {} was AVAILABLE instead of ASSIGNED, setting to ASSIGNED", driverId);
            driver.setStatus(DriverStatus.ASSIGNED);
        }
        
        // Now set to ON_TRIP
        driver.setStatus(DriverStatus.ON_TRIP);
        driver.setCurrentRideId(rideId);
        
        driver = driverRepository.save(driver);
        log.info("Driver {} accepted ride {}", driverId, rideId);
        
        return driver;
    }
    
    /**
     * Release driver after trip completion
     */
    @Transactional
    @CacheEvict(value = "availableDrivers", allEntries = true)
    public Driver releaseDriver(Long driverId) {
        Driver driver = driverRepository.findById(driverId)
            .orElseThrow(() -> new RuntimeException("Driver not found: " + driverId));
        
        driver.setStatus(DriverStatus.AVAILABLE);
        driver.setCurrentRideId(null);
        
        driver = driverRepository.save(driver);
        log.info("Released driver {}", driver.getDriverId());
        
        return driver;
    }
    
    public Optional<Driver> getDriver(String driverId) {
        return driverRepository.findByDriverId(driverId);
    }
}

