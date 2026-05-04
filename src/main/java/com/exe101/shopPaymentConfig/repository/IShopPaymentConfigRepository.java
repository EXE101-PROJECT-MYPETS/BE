package com.exe101.shopPaymentConfig.repository;

import com.exe101.shopPaymentConfig.entity.ShopPaymentConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface IShopPaymentConfigRepository extends JpaRepository<ShopPaymentConfig, Long> {

    Optional<ShopPaymentConfig> findByShopId(Long shopId);

    List<ShopPaymentConfig> findByShopIdOrderByActiveDescIdAsc(Long shopId);

    List<ShopPaymentConfig> findByShopIdAndActiveOrderByIdAsc(Long shopId, Boolean active);

    Optional<ShopPaymentConfig> findByIdAndShopId(Long id, Long shopId);

    Optional<ShopPaymentConfig> findFirstByShopIdAndActiveTrueOrderByIdAsc(Long shopId);

    boolean existsByShopIdAndBankCodeAndAccountNumber(Long shopId, String bankCode, String accountNumber);

    boolean existsByShopIdAndBankCodeAndAccountNumberAndIdNot(
            Long shopId,
            String bankCode,
            String accountNumber,
            Long id
    );

    @Modifying
    @Query("""
            UPDATE ShopPaymentConfig config
            SET config.active = false
            WHERE config.shopId = :shopId
              AND (:excludedId IS NULL OR config.id <> :excludedId)
              AND config.active = true
            """)
    void deactivateOtherActiveConfigs(@Param("shopId") Long shopId, @Param("excludedId") Long excludedId);
}
