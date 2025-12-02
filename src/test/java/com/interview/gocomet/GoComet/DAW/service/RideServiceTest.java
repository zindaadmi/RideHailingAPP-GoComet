package com.interview.gocomet.GoComet.DAW.service;

import com.interview.gocomet.GoComet.DAW.dto.RideRequest;
import com.interview.gocomet.GoComet.DAW.model.Driver;
import com.interview.gocomet.GoComet.DAW.model.DriverStatus;
import com.interview.gocomet.GoComet.DAW.model.Ride;
import com.interview.gocomet.GoComet.DAW.model.RideStatus;
import com.interview.gocomet.GoComet.DAW.model.RideTier;
import com.interview.gocomet.GoComet.DAW.model.PaymentMethod;
import com.interview.gocomet.GoComet.DAW.repository.RideRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RideServiceTest {
    
    @Mock
    private RideRepository rideRepository;
    
    @Mock
    private DriverMatchingService driverMatchingService;
    
    @InjectMocks
    private RideService rideService;
    
    private RideRequest rideRequest;
    private Driver driver;
    
    @BeforeEach
    void setUp() {
        rideRequest = new RideRequest();
        rideRequest.setRiderId("RIDER-1");
        rideRequest.setPickupLatitude(28.7041);
        rideRequest.setPickupLongitude(77.1025);
        rideRequest.setDestinationLatitude(28.5355);
        rideRequest.setDestinationLongitude(77.3910);
        rideRequest.setTier(RideTier.ECONOMY);
        rideRequest.setPaymentMethod(PaymentMethod.CARD);
        
        driver = Driver.builder()
            .id(1L)
            .driverId("DRIVER-1")
            .status(DriverStatus.AVAILABLE)
            .latitude(28.7041)
            .longitude(77.1025)
            .build();
    }
    
    @Test
    void testCreateRide_Success() {
        Ride savedRide = Ride.builder()
            .id(1L)
            .rideId("RIDE-1")
            .riderId("RIDER-1")
            .status(RideStatus.PENDING)
            .build();
        
        when(rideRepository.save(any(Ride.class))).thenReturn(savedRide);
        when(driverMatchingService.matchDriver(anyDouble(), anyDouble())).thenReturn(driver);
        
        var response = rideService.createRide(rideRequest);
        
        assertNotNull(response);
        assertEquals("RIDE-1", response.getRideId());
        verify(rideRepository, atLeastOnce()).save(any(Ride.class));
    }
    
    @Test
    void testGetRide_Success() {
        Ride ride = Ride.builder()
            .id(1L)
            .rideId("RIDE-1")
            .riderId("RIDER-1")
            .status(RideStatus.MATCHED)
            .driverId(1L)
            .createdAt(LocalDateTime.now())
            .build();
        
        when(rideRepository.findByRideId("RIDE-1")).thenReturn(Optional.of(ride));
        
        var response = rideService.getRide("RIDE-1");
        
        assertNotNull(response);
        assertEquals("RIDE-1", response.getRideId());
        assertEquals(RideStatus.MATCHED, response.getStatus());
    }
    
    @Test
    void testGetRide_NotFound() {
        when(rideRepository.findByRideId("RIDE-1")).thenReturn(Optional.empty());
        
        assertThrows(RuntimeException.class, () -> rideService.getRide("RIDE-1"));
    }
}

