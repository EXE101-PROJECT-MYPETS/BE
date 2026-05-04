package com.exe101.invoice.mapper;

import com.exe101.invoice.dto.InvoiceDTO;
import com.exe101.invoice.dto.InvoiceLineDTO;
import com.exe101.invoice.entity.Invoice;
import com.exe101.invoice.entity.InvoiceStatus;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class InvoiceMapper {

    public static InvoiceDTO toDTO(Invoice entity) {
        return toDTO(entity, List.of());
    }

    public static InvoiceDTO toDTO(Invoice entity, List<InvoiceLineDTO> lines) {
        if (entity == null) return null;
        return new InvoiceDTO(
                entity.getId(),
                entity.getShopId(),
                entity.getCustomerId(),
                entity.getBookingId(),
                entity.getOrderId(),
                entity.getTotalAmount(),
                entity.getStatus(),
                entity.getPaymentMethod(),
                entity.getIssuedAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                lines != null ? lines : List.of()
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
        entity.setPaymentMethod(dto.getPaymentMethod());
        entity.setIssuedAt(dto.getIssuedAt());
    }
}
