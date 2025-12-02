package com.interview.gocomet.GoComet.DAW.dto;

import com.interview.gocomet.GoComet.DAW.model.RideStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RideResponse {
    private String rideId;
    private String riderId;
    private RideStatus status;
    private String driverId;  // Changed to String to return driverId like "DRIVER-1" instead of numeric ID
    private Long tripId;
    private LocalDateTime createdAt;
    private LocalDateTime matchedAt;
    private LocalDateTime acceptedAt;
}

