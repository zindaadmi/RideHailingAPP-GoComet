package com.interview.gocomet.GoComet.DAW.repository;

import com.interview.gocomet.GoComet.DAW.model.Ride;
import com.interview.gocomet.GoComet.DAW.model.RideStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RideRepository extends JpaRepository<Ride, Long> {
    Optional<Ride> findByRideId(String rideId);
    
    List<Ride> findByRiderId(String riderId);
    
    List<Ride> findByDriverId(Long driverId);
    
    List<Ride> findByStatus(RideStatus status);
    
    Optional<Ride> findByIdempotencyKey(String idempotencyKey);
}

