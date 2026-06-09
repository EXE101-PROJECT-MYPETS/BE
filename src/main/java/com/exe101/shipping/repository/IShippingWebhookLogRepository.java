package com.exe101.shipping.repository;

import com.exe101.shipping.entity.ShippingWebhookLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IShippingWebhookLogRepository extends JpaRepository<ShippingWebhookLog, Long> {

    List<ShippingWebhookLog> findByOrderIdOrderByActionTimeDescIdDesc(Long orderId);
}
