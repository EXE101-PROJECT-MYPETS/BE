package com.exe101.product.repository;

import com.exe101.product.entity.ProductCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface IProductCategoryRepository extends JpaRepository<ProductCategory, Long> {

    List<ProductCategory> findByShopIdOrderBySortOrderAscNameAsc(Long shopId);

    List<ProductCategory> findByShopIdAndActiveOrderBySortOrderAscNameAsc(Long shopId, Boolean active);

    Optional<ProductCategory> findByIdAndShopId(Long id, Long shopId);

    Optional<ProductCategory> findByIdAndShopIdAndActiveTrue(Long id, Long shopId);

    boolean existsByShopIdAndName(Long shopId, String name);

    boolean existsByShopIdAndNameAndIdNot(Long shopId, String name, Long id);

    boolean existsByIdAndShopId(Long id, Long shopId);
}
