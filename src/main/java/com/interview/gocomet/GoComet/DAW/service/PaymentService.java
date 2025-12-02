package com.interview.gocomet.GoComet.DAW.service;

import com.interview.gocomet.GoComet.DAW.dto.PaymentRequest;
import com.interview.gocomet.GoComet.DAW.dto.PaymentResponse;
import com.interview.gocomet.GoComet.DAW.model.Payment;
import com.interview.gocomet.GoComet.DAW.model.PaymentStatus;
import com.interview.gocomet.GoComet.DAW.model.Ride;
import com.interview.gocomet.GoComet.DAW.model.Trip;
import com.interview.gocomet.GoComet.DAW.repository.PaymentRepository;
import com.interview.gocomet.GoComet.DAW.repository.RideRepository;
import com.interview.gocomet.GoComet.DAW.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {
    
    private final PaymentRepository paymentRepository;
    private final TripRepository tripRepository;
    private final RideRepository rideRepository;
    
    /**
     * Process payment for a trip
     * Integrates with external PSP (Payment Service Provider)
     */
    @Transactional
    public PaymentResponse processPayment(PaymentRequest request) {
        // Check idempotency
        if (request.getIdempotencyKey() != null) {
            // In a real implementation, check idempotency key in a separate table
            // For now, we'll check if payment already exists for this trip
            Optional<Payment> existingPayment = paymentRepository.findByTripId(request.getTripId())
                .stream()
                .filter(p -> p.getStatus() == PaymentStatus.SUCCESS)
                .findFirst();
            
            if (existingPayment.isPresent()) {
                log.info("Payment already processed for trip {}", request.getTripId());
                return mapToResponse(existingPayment.get());
            }
        }
        
        // Get trip details
        Trip trip = tripRepository.findById(request.getTripId())
            .orElseThrow(() -> new RuntimeException("Trip not found: " + request.getTripId()));
        
        if (trip.getStatus() != com.interview.gocomet.GoComet.DAW.model.TripStatus.COMPLETED) {
            throw new IllegalStateException("Trip must be completed before payment");
        }
        
        // Get ride to get payment method
        Ride ride = rideRepository.findById(trip.getRideId())
            .orElseThrow(() -> new RuntimeException("Ride not found: " + trip.getRideId()));
        
        // Create payment record
        Payment payment = Payment.builder()
            .paymentId("PAY-" + UUID.randomUUID().toString())
            .tripId(request.getTripId())
            .riderId(trip.getRiderId())
            .amount(trip.getTotalFare())
            .paymentMethod(ride.getPaymentMethod())
            .status(PaymentStatus.PENDING)
            .build();
        
        payment = paymentRepository.save(payment);
        
        // Process payment with external PSP (simulated)
        try {
            payment.setStatus(PaymentStatus.PROCESSING);
            payment = paymentRepository.save(payment);
            
            // Simulate PSP call
            boolean paymentSuccess = callPaymentServiceProvider(payment);
            
            if (paymentSuccess) {
                payment.setStatus(PaymentStatus.SUCCESS);
                payment.setPspTransactionId("PSP-" + UUID.randomUUID().toString());
                payment.setPspResponse("Payment successful");
                payment.setCompletedAt(LocalDateTime.now());
                log.info("Payment {} successful for trip {}", payment.getPaymentId(), request.getTripId());
            } else {
                payment.setStatus(PaymentStatus.FAILED);
                payment.setPspResponse("Payment failed");
                log.warn("Payment {} failed for trip {}", payment.getPaymentId(), request.getTripId());
            }
            
            payment = paymentRepository.save(payment);
        } catch (Exception e) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setPspResponse("Error: " + e.getMessage());
            payment = paymentRepository.save(payment);
            log.error("Error processing payment {}: {}", payment.getPaymentId(), e.getMessage(), e);
        }
        
        return mapToResponse(payment);
    }
    
    /**
     * Simulate external PSP call
     * In production, this would call actual payment gateway
     */
    private boolean callPaymentServiceProvider(Payment payment) {
        // Simulate network delay
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Simulate 95% success rate
        return Math.random() > 0.05;
    }
    
    /**
     * Get payment by payment ID
     */
    public PaymentResponse getPayment(String paymentId) {
        Payment payment = paymentRepository.findByPaymentId(paymentId)
            .orElseThrow(() -> new RuntimeException("Payment not found: " + paymentId));
        
        return mapToResponse(payment);
    }
    
    private PaymentResponse mapToResponse(Payment payment) {
        return PaymentResponse.builder()
            .paymentId(payment.getPaymentId())
            .tripId(payment.getTripId())
            .amount(payment.getAmount())
            .status(payment.getStatus())
            .pspTransactionId(payment.getPspTransactionId())
            .createdAt(payment.getCreatedAt())
            .build();
    }
}

