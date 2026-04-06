package com.exe101.resource.repository;

import com.exe101.resource.entity.ShopResource;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IShopResourceRepository extends JpaRepository<ShopResource, Long> {
}
