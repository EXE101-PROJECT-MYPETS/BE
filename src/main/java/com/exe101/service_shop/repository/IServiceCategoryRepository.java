package com.exe101.service_shop.repository;

import com.exe101.service_shop.entity.ServiceCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface IServiceCategoryRepository extends JpaRepository<ServiceCategory, Long> {

    List<ServiceCategory> findByShopIdOrderBySortOrderAscNameAsc(Long shopId);

    List<ServiceCategory> findByShopIdAndActiveOrderBySortOrderAscNameAsc(Long shopId, Boolean active);

    Optional<ServiceCategory> findByIdAndShopId(Long id, Long shopId);

    boolean existsByShopIdAndName(Long shopId, String name);

    boolean existsByShopIdAndNameAndIdNot(Long shopId, String name, Long id);
}
