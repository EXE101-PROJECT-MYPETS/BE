package com.exe101.order.repository;

import com.exe101.order.entity.OrderStatusEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IOrderStatusEventRepository extends JpaRepository<OrderStatusEvent, Long> {
}
