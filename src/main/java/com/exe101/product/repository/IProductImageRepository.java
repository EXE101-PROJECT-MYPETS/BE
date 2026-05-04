package com.exe101.product.repository;

import com.exe101.product.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IProductImageRepository extends JpaRepository<ProductImage, Long> {

    List<ProductImage> findByShopIdAndProductIdInOrderByProductIdAscSortOrderAscIdAsc(Long shopId, List<Long> productIds);

    void deleteByShopIdAndProductId(Long shopId, Long productId);
}
