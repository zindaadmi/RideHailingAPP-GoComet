package com.interview.gocomet.GoComet.DAW.repository;

import com.interview.gocomet.GoComet.DAW.model.Driver;
import com.interview.gocomet.GoComet.DAW.model.DriverStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DriverRepository extends JpaRepository<Driver, Long> {
    Optional<Driver> findByDriverId(String driverId);
    
    List<Driver> findByStatus(DriverStatus status);
    
    @Query(value = "SELECT * FROM drivers d " +
           "WHERE d.status = 'AVAILABLE' " +
           "AND d.latitude BETWEEN :minLat AND :maxLat " +
           "AND d.longitude BETWEEN :minLng AND :maxLng " +
           "ORDER BY " +
           "(6371 * acos(cos(radians(:lat)) * cos(radians(d.latitude)) * " +
           "cos(radians(d.longitude) - radians(:lng)) + " +
           "sin(radians(:lat)) * sin(radians(d.latitude)))) " +
           "LIMIT :limit", nativeQuery = true)
    List<Driver> findNearbyAvailableDrivers(
        @Param("lat") Double latitude,
        @Param("lng") Double longitude,
        @Param("minLat") Double minLat,
        @Param("maxLat") Double maxLat,
        @Param("minLng") Double minLng,
        @Param("maxLng") Double maxLng,
        @Param("limit") Integer limit
    );
}

