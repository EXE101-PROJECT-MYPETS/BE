package com.exe101.order.controller;

import com.exe101.auth.model.UserPrincipal;
import com.exe101.common.ScrollResponse;
import com.exe101.order.dto.OrderDTO;
import com.exe101.order.dto.OrderListItemDTO;
import com.exe101.order.entity.OrderSource;
import com.exe101.order.entity.OrderStatus;
import com.exe101.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping
    public ResponseEntity<ScrollResponse<OrderListItemDTO>> getAll(
            @RequestHeader("X-Shop-Id") Long shopId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) OrderSource source,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate createdDate,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(orderService.getAllForScroll(
                shopId,
                userId,
                customerId,
                status,
                source,
                createdDate,
                cursor,
                size
        ));
    }

    @GetMapping("/customer")
    public ResponseEntity<ScrollResponse<OrderListItemDTO>> getCustomerOrders(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(orderService.getAllForScroll(
                null,
            getCurrentUserId(principal),
                null,
                status,
                null,
                null,
                cursor,
                size
        ));
    }

    @GetMapping("/customer/{id}")
    public ResponseEntity<OrderListItemDTO> getCustomerOrderDetail(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(orderService.getCustomerOrderDetail(getCurrentUserId(principal), id));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderListItemDTO> getById(
            @RequestHeader("X-Shop-Id") Long shopId,
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(orderService.getDetail(shopId, id));
    }

    @PostMapping
    public ResponseEntity<OrderDTO> create(
            @RequestHeader("X-Shop-Id") Long shopId,
            @Valid @RequestBody OrderDTO dto
    ) {
        dto.setShopId(shopId);
        return ResponseEntity.ok(orderService.create(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<OrderDTO> update(
            @RequestHeader("X-Shop-Id") Long shopId,
            @PathVariable Long id,
            @Valid @RequestBody OrderDTO dto
    ) {
        dto.setShopId(shopId);
        return ResponseEntity.ok(orderService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        orderService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private Long getCurrentUserId(UserPrincipal principal) {
        if (principal == null || principal.getUser() == null || principal.getUser().getId() == null) {
            throw new IllegalStateException("Cần đăng nhập để thực hiện chức năng này");
        }
        return principal.getUser().getId();
    }
}
