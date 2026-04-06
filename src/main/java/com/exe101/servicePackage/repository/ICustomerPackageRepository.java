package com.exe101.servicePackage.repository;

import com.exe101.servicePackage.entity.CustomerPackage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ICustomerPackageRepository extends JpaRepository<CustomerPackage, Long> {
}
