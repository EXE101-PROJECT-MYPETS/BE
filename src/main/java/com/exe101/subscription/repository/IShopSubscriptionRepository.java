package com.exe101.subscription.repository;

import com.exe101.subscription.entity.ShopSubscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IShopSubscriptionRepository extends JpaRepository<ShopSubscription, Long> {
    Optional<ShopSubscription> findByShopId(Long shopId);

    boolean existsByShopId(Long shopId);
}
