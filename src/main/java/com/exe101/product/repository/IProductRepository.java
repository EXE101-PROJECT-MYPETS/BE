package com.exe101.product.repository;

import com.exe101.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByShopIdOrderByIdDesc(Long shopId);
}
