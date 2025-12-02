package com.interview.gocomet.GoComet.DAW.service;

import com.interview.gocomet.GoComet.DAW.model.Driver;
import com.interview.gocomet.GoComet.DAW.model.DriverStatus;
import com.interview.gocomet.GoComet.DAW.repository.DriverRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DriverMatchingServiceTest {
    
    @Mock
    private DriverRepository driverRepository;
    
    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    
    @InjectMocks
    private DriverMatchingService driverMatchingService;
    
    private List<Driver> availableDrivers;
    
    @BeforeEach
    void setUp() {
        availableDrivers = Arrays.asList(
            Driver.builder()
                .id(1L)
                .driverId("DRIVER-1")
                .status(DriverStatus.AVAILABLE)
                .latitude(28.7041)
                .longitude(77.1025)
                .lastLocationUpdate(LocalDateTime.now())
                .build(),
            Driver.builder()
                .id(2L)
                .driverId("DRIVER-2")
                .status(DriverStatus.AVAILABLE)
                .latitude(28.7050)
                .longitude(77.1030)
                .lastLocationUpdate(LocalDateTime.now())
                .build()
        );
    }
    
    @Test
    void testFindAvailableDrivers_Success() {
        when(driverRepository.findNearbyAvailableDrivers(
            anyDouble(), anyDouble(), anyDouble(), anyDouble(),
            anyDouble(), anyDouble(), anyInt()
        )).thenReturn(availableDrivers);
        
        List<Driver> drivers = driverMatchingService.findAvailableDrivers(28.7041, 77.1025);
        
        assertNotNull(drivers);
        assertFalse(drivers.isEmpty());
    }
    
    @Test
    void testMatchDriver_Success() {
        when(driverRepository.findNearbyAvailableDrivers(
            anyDouble(), anyDouble(), anyDouble(), anyDouble(),
            anyDouble(), anyDouble(), anyInt()
        )).thenReturn(availableDrivers);
        
        when(driverRepository.findById(1L)).thenReturn(java.util.Optional.of(availableDrivers.get(0)));
        when(driverRepository.save(any(Driver.class))).thenReturn(availableDrivers.get(0));
        
        Driver matched = driverMatchingService.matchDriver(28.7041, 77.1025);
        
        assertNotNull(matched);
        assertEquals(DriverStatus.ASSIGNED, matched.getStatus());
    }
}

