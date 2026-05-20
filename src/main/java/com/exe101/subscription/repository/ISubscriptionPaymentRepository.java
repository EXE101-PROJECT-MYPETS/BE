package com.exe101.subscription.repository;

import com.exe101.subscription.entity.SubscriptionPayment;
import com.exe101.subscription.entity.SubscriptionPaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface ISubscriptionPaymentRepository extends JpaRepository<SubscriptionPayment, Long> {
    List<SubscriptionPayment> findByShopIdOrderByCreatedAtDesc(Long shopId);

    Page<SubscriptionPayment> findByShopIdOrderByCreatedAtDesc(Long shopId, Pageable pageable);

    Optional<SubscriptionPayment> findByShopIdAndId(Long shopId, Long id);

    Optional<SubscriptionPayment> findFirstByShopIdAndStatusOrderByCreatedAtDesc(
            Long shopId,
            SubscriptionPaymentStatus status
    );

    Optional<SubscriptionPayment> findFirstByShopIdAndStatusAndExpiredAtAfterOrderByCreatedAtDesc(
            Long shopId,
            SubscriptionPaymentStatus status,
            OffsetDateTime now
    );

    Optional<SubscriptionPayment> findFirstByProviderAndProviderTransactionId(
            String provider,
            String providerTransactionId
    );

    Optional<SubscriptionPayment> findFirstByProviderAndProviderTransactionIdAndStatus(
            String provider,
            String providerTransactionId,
            SubscriptionPaymentStatus status
    );

    @Modifying
    @Query(value = """
            UPDATE subscription_payments
            SET status = 'EXPIRED'::subscription_payment_status,
                updated_at = now()
            WHERE shop_id = :shopId
              AND status = 'PENDING'::subscription_payment_status
              AND expired_at < :now
            """, nativeQuery = true)
    int expirePendingPaymentsByShopId(
            @Param("shopId") Long shopId,
            @Param("now") OffsetDateTime now
    );

    @Modifying
    @Query(value = """
            UPDATE subscription_payments
            SET status = 'CANCELED'::subscription_payment_status,
                updated_at = now()
            WHERE shop_id = :shopId
              AND status = 'PENDING'::subscription_payment_status
              AND expired_at > :now
            """, nativeQuery = true)
    int cancelActivePendingPaymentsByShopId(
            @Param("shopId") Long shopId,
            @Param("now") OffsetDateTime now
    );

    @Query(value = """
            SELECT *
            FROM subscription_payments p
            WHERE :content ILIKE CONCAT('%', p.transfer_content, '%')
               OR :content ILIKE CONCAT('%', p.invoice_number, '%')
               OR regexp_replace(:content, '[^A-Za-z0-9]', '', 'g')
                    ILIKE CONCAT('%', regexp_replace(p.transfer_content, '[^A-Za-z0-9]', '', 'g'), '%')
               OR regexp_replace(:content, '[^A-Za-z0-9]', '', 'g')
                    ILIKE CONCAT('%', regexp_replace(p.invoice_number, '[^A-Za-z0-9]', '', 'g'), '%')
            ORDER BY p.created_at DESC
            LIMIT 1
            """, nativeQuery = true)
    Optional<SubscriptionPayment> findLatestBySearchableContent(@Param("content") String content);
}
