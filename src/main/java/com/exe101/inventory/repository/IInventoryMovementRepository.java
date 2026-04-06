package com.exe101.inventory.repository;

import com.exe101.inventory.entity.InventoryMovement;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IInventoryMovementRepository extends JpaRepository<InventoryMovement, Long> {
}
