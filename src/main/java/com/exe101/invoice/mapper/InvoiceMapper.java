package com.exe101.invoice.mapper;

import com.exe101.invoice.dto.InvoiceDTO;
import com.exe101.invoice.entity.Invoice;
import com.exe101.invoice.entity.InvoiceStatus;
import org.springframework.stereotype.Component;

@Component
public class InvoiceMapper {

    public static InvoiceDTO toDTO(Invoice entity) {
        if (entity == null) return null;
        return new InvoiceDTO(
                entity.getId(),
                entity.getShopId(),
                entity.getCustomerId(),
                entity.getBookingId(),
                entity.getOrderId(),
                entity.getTotalAmount(),
                entity.getStatus(),
                entity.getIssuedAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public static Invoice toEntity(InvoiceDTO dto) {
        if (dto == null) return null;
        Invoice entity = new Invoice();
        updateEntity(entity, dto);
        return entity;
    }

    public static void updateEntity(Invoice entity, InvoiceDTO dto) {
        entity.setShopId(dto.getShopId());
        entity.setCustomerId(dto.getCustomerId());
        entity.setBookingId(dto.getBookingId());
        entity.setOrderId(dto.getOrderId());
        entity.setTotalAmount(dto.getTotalAmount() != null ? dto.getTotalAmount() : 0L);
        entity.setStatus(dto.getStatus() != null ? dto.getStatus() : InvoiceStatus.DRAFT);
        entity.setIssuedAt(dto.getIssuedAt());
    }
}
