package com.exe101.order.repository;

import com.exe101.order.entity.OrderCancelRequest;
import com.exe101.order.entity.OrderCancelRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface IOrderCancelRequestRepository extends JpaRepository<OrderCancelRequest, Long> {

    Optional<OrderCancelRequest> findByIdAndShopId(Long id, Long shopId);

    Optional<OrderCancelRequest> findFirstByOrderIdAndStatusOrderByCreatedAtDescIdDesc(
            Long orderId,
            OrderCancelRequestStatus status
    );

    Optional<OrderCancelRequest> findFirstByOrderIdOrderByCreatedAtDescIdDesc(Long orderId);

    List<OrderCancelRequest> findByOrderIdInOrderByOrderIdAscCreatedAtDescIdDesc(List<Long> orderIds);

    List<OrderCancelRequest> findByShopIdOrderByCreatedAtDescIdDesc(Long shopId);

    List<OrderCancelRequest> findByShopIdAndStatusOrderByCreatedAtDescIdDesc(
            Long shopId,
            OrderCancelRequestStatus status
    );
}
