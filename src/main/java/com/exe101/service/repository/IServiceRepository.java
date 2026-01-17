package com.exe101.service.repository;

import com.exe101.shop.entity.Shop;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IServiceRepository extends JpaRepository<Shop, Long> {
}
