package com.interview.gocomet.GoComet.DAW.repository;

import com.interview.gocomet.GoComet.DAW.model.Trip;
import com.interview.gocomet.GoComet.DAW.model.TripStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TripRepository extends JpaRepository<Trip, Long> {
    Optional<Trip> findByTripId(String tripId);
    
    List<Trip> findByRideId(Long rideId);
    
    List<Trip> findByDriverId(Long driverId);
    
    List<Trip> findByStatus(TripStatus status);
}

