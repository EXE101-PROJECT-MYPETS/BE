package com.exe101.resource.repository;

import com.exe101.resource.entity.ShopResource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IShopResourceRepository extends JpaRepository<ShopResource, Long> {
    List<ShopResource> findByShopIdOrderByIdDesc(Long shopId);
}
