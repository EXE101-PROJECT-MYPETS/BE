package com.exe101.service_shop.repository;

import com.exe101.service_shop.entity.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IServiceRepository extends JpaRepository<Service, Long> {

    Page<Service> findByNameContainingIgnoreCase(String name, Pageable pageable);

    boolean existsByShopIdAndName(Long shopId, String name);

    boolean existsByShopIdAndNameAndIdNot(Long shopId, String name, Long id);
}
