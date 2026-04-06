package com.exe101.payment.mapper;

import com.exe101.payment.dto.PaymentIntentDTO;
import com.exe101.payment.entity.PaymentIntent;
import com.exe101.payment.entity.PaymentIntentStatus;
import org.springframework.stereotype.Component;

@Component
public class PaymentIntentMapper {

    public static PaymentIntentDTO toDTO(PaymentIntent entity) {
        if (entity == null) return null;
        return new PaymentIntentDTO(
                entity.getId(),
                entity.getShopId(),
                entity.getInvoiceId(),
                entity.getProvider(),
                entity.getMethod(),
                entity.getAmount(),
                entity.getCurrency(),
                entity.getStatus(),
                entity.getCreatedAt()
        );
    }

    public static PaymentIntent toEntity(PaymentIntentDTO dto) {
        if (dto == null) return null;
        PaymentIntent entity = new PaymentIntent();
        entity.setShopId(dto.getShopId());
        entity.setInvoiceId(dto.getInvoiceId());
        entity.setProvider(dto.getProvider());
        entity.setMethod(dto.getMethod() != null ? dto.getMethod() : "UNKNOWN");
        entity.setAmount(dto.getAmount());
        entity.setCurrency(dto.getCurrency() != null ? dto.getCurrency() : "VND");
        entity.setStatus(dto.getStatus() != null ? dto.getStatus() : PaymentIntentStatus.REQUIRES_PAYMENT_METHOD);
        return entity;
    }
}
