package com.interview.gocomet.GoComet.DAW.repository;

import com.interview.gocomet.GoComet.DAW.model.Payment;
import com.interview.gocomet.GoComet.DAW.model.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByPaymentId(String paymentId);
    
    List<Payment> findByTripId(Long tripId);
    
    List<Payment> findByRiderId(String riderId);
    
    List<Payment> findByStatus(PaymentStatus status);
}

