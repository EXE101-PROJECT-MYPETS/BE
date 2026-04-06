package com.exe101.payment.repository;

import com.exe101.payment.entity.PaymentIntent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IPaymentIntentRepository extends JpaRepository<PaymentIntent, Long> {
}
