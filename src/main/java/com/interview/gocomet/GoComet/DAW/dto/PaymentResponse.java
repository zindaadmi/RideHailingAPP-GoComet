package com.interview.gocomet.GoComet.DAW.dto;

import com.interview.gocomet.GoComet.DAW.model.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {
    private String paymentId;
    private Long tripId;
    private Double amount;
    private PaymentStatus status;
    private String pspTransactionId;
    private LocalDateTime createdAt;
}

