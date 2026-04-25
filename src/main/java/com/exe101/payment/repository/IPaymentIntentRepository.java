package com.exe101.payment.repository;

import com.exe101.payment.entity.PaymentIntent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IPaymentIntentRepository extends JpaRepository<PaymentIntent, Long> {
    List<PaymentIntent> findByShopIdOrderByIdDesc(Long shopId);
}
