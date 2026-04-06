package com.exe101.payment.mapper;

import com.exe101.payment.dto.PaymentTransactionDTO;
import com.exe101.payment.entity.PaymentTransaction;
import org.springframework.stereotype.Component;

@Component
public class PaymentTransactionMapper {

    public static PaymentTransactionDTO toDTO(PaymentTransaction entity) {
        if (entity == null) return null;
        return new PaymentTransactionDTO(
                entity.getId(),
                entity.getShopId(),
                entity.getPaymentIntentId(),
                entity.getProviderTxnId(),
                entity.getStatus(),
                entity.getRawPayloadJson(),
                entity.getCreatedAt()
        );
    }

    public static PaymentTransaction toEntity(PaymentTransactionDTO dto) {
        if (dto == null) return null;
        PaymentTransaction entity = new PaymentTransaction();
        entity.setShopId(dto.getShopId());
        entity.setPaymentIntentId(dto.getPaymentIntentId());
        entity.setProviderTxnId(dto.getProviderTxnId());
        entity.setStatus(dto.getStatus());
        entity.setRawPayloadJson(dto.getRawPayloadJson());
        return entity;
    }
}
