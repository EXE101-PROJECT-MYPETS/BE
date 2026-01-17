package com.exe101.shop.repository;

import com.exe101.shop.entity.Shop;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IShopRepository extends JpaRepository<Shop, Long> {
}
