package com.interview.gocomet.GoComet.DAW.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PaymentRequest {
    @NotNull(message = "Trip ID is required")
    private Long tripId;
    
    private String idempotencyKey;
}

