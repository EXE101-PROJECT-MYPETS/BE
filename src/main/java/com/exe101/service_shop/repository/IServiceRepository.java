package com.exe101.service_shop.repository;

import com.exe101.service_shop.entity.Service;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IServiceRepository extends JpaRepository<Service, Long> {
}
