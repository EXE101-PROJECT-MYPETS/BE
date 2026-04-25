package com.exe101.inventory.repository;

import com.exe101.inventory.entity.Inventory;
import com.exe101.inventory.entity.InventoryId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IInventoryRepository extends JpaRepository<Inventory, InventoryId> {
    List<Inventory> findByShopId(Long shopId);
}
