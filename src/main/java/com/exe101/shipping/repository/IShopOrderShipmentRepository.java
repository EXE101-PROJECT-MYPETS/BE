package com.exe101.shipping.repository;

import com.exe101.shipping.entity.ShopOrderShipment;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface IShopOrderShipmentRepository extends JpaRepository<ShopOrderShipment, Long> {

    Optional<ShopOrderShipment> findByOrderId(Long orderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT s
            FROM ShopOrderShipment s
            WHERE s.orderId = :orderId
            """)
    Optional<ShopOrderShipment> findByOrderIdForUpdate(@Param("orderId") Long orderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT s
            FROM ShopOrderShipment s
            WHERE s.partnerId = :partnerId
            """)
    Optional<ShopOrderShipment> findByPartnerIdForUpdate(@Param("partnerId") String partnerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT s
            FROM ShopOrderShipment s
            WHERE s.labelId = :labelId
            """)
    Optional<ShopOrderShipment> findByLabelIdForUpdate(@Param("labelId") String labelId);
}
