package com.interview.gocomet.GoComet.DAW.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "trips", indexes = {
    @Index(name = "idx_trip_ride", columnList = "rideId"),
    @Index(name = "idx_trip_status", columnList = "status"),
    @Index(name = "idx_trip_driver", columnList = "driverId")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Trip {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String tripId;
    
    @Column(nullable = false)
    private Long rideId;
    
    @Column(nullable = false)
    private Long driverId;
    
    @Column(nullable = false)
    private String riderId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TripStatus status;
    
    @Column(nullable = false)
    private Double startLatitude;
    
    @Column(nullable = false)
    private Double startLongitude;
    
    @Column(nullable = false)
    private Double endLatitude;
    
    @Column(nullable = false)
    private Double endLongitude;
    
    private LocalDateTime startTime;
    
    private LocalDateTime endTime;
    
    private LocalDateTime pauseStartTime;
    
    private Long totalPauseDurationSeconds;
    
    private Double distanceKm;
    
    private Double durationMinutes;
    
    private Double baseFare;
    
    private Double distanceFare;
    
    private Double timeFare;
    
    private Double surgeMultiplier;
    
    private Double totalFare;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (tripId == null) {
            tripId = "TRIP-" + System.currentTimeMillis() + "-" + id;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

