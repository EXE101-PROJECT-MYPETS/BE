package com.exe101.customer.repository;

import com.exe101.customer.entity.Customer;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ICustomerRepository extends JpaRepository<Customer, Long> {
    List<Customer> findByShopIdOrderByIdDesc(Long shopId);

    Optional<Customer> findByShopIdAndId(Long shopId, Long id);

    Optional<Customer> findFirstByShopIdAndUserIdOrderByIdDesc(Long shopId, Long userId);

    Optional<Customer> findByShopIdAndPhone(Long shopId, String phone);

    List<Customer> findByShopIdAndPhoneStartingWithOrderByIdDesc(Long shopId, String phone, Pageable pageable);
}
