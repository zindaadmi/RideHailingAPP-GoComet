package com.interview.gocomet.GoComet.DAW.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "rides", indexes = {
    @Index(name = "idx_ride_status", columnList = "status"),
    @Index(name = "idx_ride_rider", columnList = "riderId"),
    @Index(name = "idx_ride_driver", columnList = "driverId"),
    @Index(name = "idx_ride_created", columnList = "createdAt")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Ride {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String rideId;
    
    @Column(nullable = false)
    private String riderId;
    
    @Column(nullable = false)
    private Double pickupLatitude;
    
    @Column(nullable = false)
    private Double pickupLongitude;
    
    @Column(nullable = false)
    private Double destinationLatitude;
    
    @Column(nullable = false)
    private Double destinationLongitude;
    
    private String pickupAddress;
    
    private String destinationAddress;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RideTier tier;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod paymentMethod;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RideStatus status;
    
    private Long driverId;
    
    private Long tripId;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    private LocalDateTime matchedAt;
    
    private LocalDateTime acceptedAt;
    
    @Column(unique = true)
    private String idempotencyKey;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (rideId == null) {
            rideId = "RIDE-" + System.currentTimeMillis() + "-" + id;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

