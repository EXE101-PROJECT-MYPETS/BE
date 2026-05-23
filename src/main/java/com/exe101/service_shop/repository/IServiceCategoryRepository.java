package com.exe101.service_shop.repository;

import com.exe101.service_shop.entity.ServiceCategory;
import com.exe101.service_shop.entity.ServiceType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface IServiceCategoryRepository extends JpaRepository<ServiceCategory, Long> {

    List<ServiceCategory> findByShopIdOrderBySortOrderAscNameAsc(Long shopId);

    List<ServiceCategory> findByShopIdAndServiceTypeOrderBySortOrderAscNameAsc(Long shopId, ServiceType serviceType);

    List<ServiceCategory> findByShopIdAndActiveOrderBySortOrderAscNameAsc(Long shopId, Boolean active);

    List<ServiceCategory> findByShopIdAndServiceTypeAndActiveOrderBySortOrderAscNameAsc(Long shopId, ServiceType serviceType, Boolean active);

    Optional<ServiceCategory> findByIdAndShopId(Long id, Long shopId);

    boolean existsByShopIdAndNameAndServiceType(Long shopId, String name, ServiceType serviceType);

    boolean existsByShopIdAndNameAndServiceTypeAndIdNot(Long shopId, String name, ServiceType serviceType, Long id);
}
