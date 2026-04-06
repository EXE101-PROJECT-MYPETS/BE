package com.exe101.servicePackage.repository;

import com.exe101.servicePackage.entity.PackageLedger;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IPackageLedgerRepository extends JpaRepository<PackageLedger, Long> {
}
