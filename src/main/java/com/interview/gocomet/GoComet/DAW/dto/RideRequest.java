package com.interview.gocomet.GoComet.DAW.dto;

import com.interview.gocomet.GoComet.DAW.model.PaymentMethod;
import com.interview.gocomet.GoComet.DAW.model.RideTier;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RideRequest {
    @NotNull(message = "Rider ID is required")
    private String riderId;
    
    @NotNull(message = "Pickup latitude is required")
    private Double pickupLatitude;
    
    @NotNull(message = "Pickup longitude is required")
    private Double pickupLongitude;
    
    @NotNull(message = "Destination latitude is required")
    private Double destinationLatitude;
    
    @NotNull(message = "Destination longitude is required")
    private Double destinationLongitude;
    
    private String pickupAddress;
    
    private String destinationAddress;
    
    @NotNull(message = "Tier is required")
    private RideTier tier;
    
    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;
    
    private String idempotencyKey;
}

