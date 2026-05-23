package com.exe101.subscription.repository;

import com.exe101.subscription.entity.SubscriptionPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ISubscriptionPlanRepository extends JpaRepository<SubscriptionPlan, Long> {
    List<SubscriptionPlan> findByActiveTrueOrderByDurationMonthsAscIdAsc();
}
