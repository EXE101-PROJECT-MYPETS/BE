package com.exe101.payment.repository;

import com.exe101.payment.entity.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IPaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {
}
