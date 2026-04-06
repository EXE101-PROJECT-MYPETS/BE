package com.exe101.inventory.repository;

import com.exe101.inventory.entity.Inventory;
import com.exe101.inventory.entity.InventoryId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IInventoryRepository extends JpaRepository<Inventory, InventoryId> {
}
