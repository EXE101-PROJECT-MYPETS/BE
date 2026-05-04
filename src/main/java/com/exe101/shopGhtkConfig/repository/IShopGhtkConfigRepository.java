package com.exe101.shopGhtkConfig.repository;

import com.exe101.shopGhtkConfig.entity.ShopGhtkConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IShopGhtkConfigRepository extends JpaRepository<ShopGhtkConfig, Long> {

    Optional<ShopGhtkConfig> findByShopId(Long shopId);
}
