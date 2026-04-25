package com.exe101.servicePackage.repository;

import com.exe101.servicePackage.entity.ServicePackage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IServicePackageRepository extends JpaRepository<ServicePackage, Long> {
    List<ServicePackage> findByShopIdOrderByIdDesc(Long shopId);
}
