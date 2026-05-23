package com.exe101.order.mapper;

import com.exe101.order.dto.OrderDTO;
import com.exe101.order.dto.OrderItemDTO;
import com.exe101.order.dto.OrderListItemDTO;
import com.exe101.order.dto.OrderShippingSnapshotDTO;
import com.exe101.order.entity.CustomerOrder;
import com.exe101.order.entity.OrderSource;
import com.exe101.order.entity.OrderStatus;
import com.exe101.user.entity.User;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OrderMapper {

    public static OrderDTO toDTO(CustomerOrder entity, List<OrderItemDTO> items) {
        if (entity == null) return null;
        return new OrderDTO(
                entity.getId(),
                entity.getOrderCode(),
                entity.getShopId(),
                entity.getUserId(),
                entity.getCustomerId(),
                entity.getCustomerAddressId(),
                entity.getUserAddressId(),
                entity.getStatus(),
                entity.getSource(),
                entity.getSubtotalAmount(),
                entity.getShippingFee(),
                entity.getDiscountAmount(),
                entity.getTotalAmount(),
                entity.getReceiverName(),
                entity.getReceiverPhone(),
                entity.getShippingAddress(),
                entity.getShippingProvince(),
                entity.getShippingDistrict(),
                entity.getShippingWard(),
                entity.getShippingStreet(),
                entity.getShippingHamlet(),
                entity.getNote(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                items
        );
    }

    public static OrderListItemDTO toListItemDTO(
            CustomerOrder entity,
            User user,
            List<OrderItemDTO> items,
            String statusLabel
    ) {
        if (entity == null) return null;
        return new OrderListItemDTO(
                entity.getId(),
                entity.getOrderCode(),
                entity.getShopId(),
                entity.getShop() != null ? entity.getShop().getName() : "Cửa hàng PetPee",
                entity.getUserId(),
                user != null ? user.getFullName() : null,
                user != null ? user.getPhone() : null,
                user != null ? user.getEmail() : null,
                user != null ? user.getAvatarUrlPreview() : null,
                entity.getUserAddressId(),
                toShippingSnapshotDTO(entity),
                items,
                entity.getTotalAmount(),
                entity.getStatus(),
                statusLabel,
                entity.getSource(),
                entity.getCreatedAt()
        );
    }

    private static OrderShippingSnapshotDTO toShippingSnapshotDTO(CustomerOrder entity) {
        return new OrderShippingSnapshotDTO(
                entity.getReceiverName(),
                entity.getReceiverPhone(),
                entity.getShippingAddress(),
                entity.getShippingProvince(),
                entity.getShippingDistrict(),
                entity.getShippingWard(),
                entity.getShippingStreet(),
                entity.getShippingHamlet()
        );
    }

    public static CustomerOrder toEntity(OrderDTO dto) {
        if (dto == null) return null;
        CustomerOrder entity = new CustomerOrder();
        entity.setShopId(dto.getShopId());
        entity.setUserId(dto.getUserId());
        entity.setCustomerId(dto.getCustomerId());
        entity.setCustomerAddressId(dto.getCustomerAddressId());
        entity.setUserAddressId(dto.getUserAddressId());
        entity.setOrderCode(dto.getOrderCode());
        entity.setStatus(dto.getStatus() != null ? dto.getStatus() : OrderStatus.PENDING);
        entity.setSource(dto.getSource() != null ? dto.getSource() : OrderSource.ONLINE);
        entity.setSubtotalAmount(dto.getSubtotalAmount() != null ? dto.getSubtotalAmount() : 0L);
        entity.setShippingFee(dto.getShippingFee() != null ? dto.getShippingFee() : 0L);
        entity.setDiscountAmount(dto.getDiscountAmount() != null ? dto.getDiscountAmount() : 0L);
        entity.setTotalAmount(dto.getTotalAmount() != null ? dto.getTotalAmount() : 0L);
        entity.setReceiverName(dto.getReceiverName());
        entity.setReceiverPhone(dto.getReceiverPhone());
        entity.setShippingAddress(dto.getShippingAddress());
        entity.setShippingProvince(dto.getShippingProvince());
        entity.setShippingDistrict(dto.getShippingDistrict());
        entity.setShippingWard(dto.getShippingWard());
        entity.setShippingStreet(dto.getShippingStreet());
        entity.setShippingHamlet(dto.getShippingHamlet());
        entity.setNote(dto.getNote());
        return entity;
    }

    public static void updateEntity(CustomerOrder entity, OrderDTO dto) {
        if (dto.getShopId() != null) {
            entity.setShopId(dto.getShopId());
        }
        if (dto.getUserId() != null) {
            entity.setUserId(dto.getUserId());
        }
        if (dto.getCustomerId() != null) {
            entity.setCustomerId(dto.getCustomerId());
        }
        if (dto.getCustomerAddressId() != null) {
            entity.setCustomerAddressId(dto.getCustomerAddressId());
        }
        if (dto.getUserAddressId() != null) {
            entity.setUserAddressId(dto.getUserAddressId());
        }
        if (dto.getOrderCode() != null) {
            entity.setOrderCode(dto.getOrderCode());
        }
        if (dto.getStatus() != null) {
            entity.setStatus(dto.getStatus());
        }
        if (dto.getSource() != null) {
            entity.setSource(dto.getSource());
        }
        if (dto.getSubtotalAmount() != null) {
            entity.setSubtotalAmount(dto.getSubtotalAmount());
        }
        if (dto.getShippingFee() != null) {
            entity.setShippingFee(dto.getShippingFee());
        }
        if (dto.getDiscountAmount() != null) {
            entity.setDiscountAmount(dto.getDiscountAmount());
        }
        if (dto.getReceiverName() != null) {
            entity.setReceiverName(dto.getReceiverName());
        }
        if (dto.getReceiverPhone() != null) {
            entity.setReceiverPhone(dto.getReceiverPhone());
        }
        if (dto.getShippingAddress() != null) {
            entity.setShippingAddress(dto.getShippingAddress());
        }
        if (dto.getShippingProvince() != null) {
            entity.setShippingProvince(dto.getShippingProvince());
        }
        if (dto.getShippingDistrict() != null) {
            entity.setShippingDistrict(dto.getShippingDistrict());
        }
        if (dto.getShippingWard() != null) {
            entity.setShippingWard(dto.getShippingWard());
        }
        if (dto.getShippingStreet() != null) {
            entity.setShippingStreet(dto.getShippingStreet());
        }
        if (dto.getShippingHamlet() != null) {
            entity.setShippingHamlet(dto.getShippingHamlet());
        }
        if (dto.getNote() != null) {
            entity.setNote(dto.getNote());
        }
    }
}
