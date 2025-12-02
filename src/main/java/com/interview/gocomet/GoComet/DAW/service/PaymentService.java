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
        // Check if payment already exists for this trip (any status)
        Optional<Payment> existingPayment = paymentRepository.findByTripId(request.getTripId())
            .stream()
            .findFirst();
        
        if (existingPayment.isPresent()) {
            Payment existing = existingPayment.get();
            // If already successful, return it
            if (existing.getStatus() == PaymentStatus.SUCCESS) {
                log.info("Payment already processed successfully for trip {}", request.getTripId());
                return mapToResponse(existing);
            }
            // If in PROCESSING or PENDING, complete it now
            if (existing.getStatus() == PaymentStatus.PROCESSING || existing.getStatus() == PaymentStatus.PENDING) {
                log.info("Completing existing payment {} for trip {}", existing.getPaymentId(), request.getTripId());
                existing.setStatus(PaymentStatus.SUCCESS);
                existing.setPspTransactionId("PSP-" + UUID.randomUUID().toString());
                existing.setPspResponse("Payment successful");
                existing.setCompletedAt(LocalDateTime.now());
                existing = paymentRepository.save(existing);
                return mapToResponse(existing);
            }
            // If failed, create a new one
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
        
        // Create payment record and process immediately
        Payment payment = Payment.builder()
            .paymentId("PAY-" + UUID.randomUUID().toString())
            .tripId(request.getTripId())
            .riderId(trip.getRiderId())
            .amount(trip.getTotalFare())
            .paymentMethod(ride.getPaymentMethod())
            .status(PaymentStatus.SUCCESS) // Set to SUCCESS immediately
            .pspTransactionId("PSP-" + UUID.randomUUID().toString())
            .pspResponse("Payment successful")
            .completedAt(LocalDateTime.now())
            .build();
        
        // Simulate PSP call (no delay needed, just for logging)
        callPaymentServiceProvider(payment);
        
        payment = paymentRepository.save(payment);
        log.info("Payment {} successful for trip {} - amount: {}", payment.getPaymentId(), request.getTripId(), payment.getAmount());
        
        return mapToResponse(payment);
    }
    
    /**
     * Simulate external PSP call
     * In production, this would call actual payment gateway
     */
    private boolean callPaymentServiceProvider(Payment payment) {
        // No delay - payment completes immediately
        // In production, this would call actual payment gateway
        log.debug("Processing payment {} via PSP", payment.getPaymentId());
        return true;
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

