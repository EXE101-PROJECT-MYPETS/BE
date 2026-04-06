package com.exe101.servicePackage.repository;

import com.exe101.servicePackage.entity.ServicePackage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IServicePackageRepository extends JpaRepository<ServicePackage, Long> {
}
