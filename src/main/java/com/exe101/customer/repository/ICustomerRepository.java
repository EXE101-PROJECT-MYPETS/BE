package com.exe101.customer.repository;

import com.exe101.customer.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ICustomerRepository extends JpaRepository<Customer, Long> {
    List<Customer> findByShopIdOrderByIdDesc(Long shopId);
}
