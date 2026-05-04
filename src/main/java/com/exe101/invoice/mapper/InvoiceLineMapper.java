package com.exe101.invoice.mapper;

import com.exe101.invoice.dto.InvoiceLineDTO;
import com.exe101.invoice.entity.InvoiceLine;
import org.springframework.stereotype.Component;

@Component
public class InvoiceLineMapper {

    public static InvoiceLineDTO toDTO(InvoiceLine entity) {
        if (entity == null) return null;
        return new InvoiceLineDTO(
                entity.getId(),
                entity.getShopId(),
                entity.getInvoiceId(),
                entity.getLineType(),
                entity.getRefId(),
                entity.getItemName(),
                entity.getQty(),
                entity.getUnitPrice(),
                entity.getAmount()
        );
    }

    public static InvoiceLine toEntity(InvoiceLineDTO dto) {
        if (dto == null) return null;
        InvoiceLine entity = new InvoiceLine();
        entity.setShopId(dto.getShopId());
        entity.setInvoiceId(dto.getInvoiceId());
        entity.setLineType(dto.getLineType());
        entity.setRefId(dto.getRefId());
        entity.setItemName(dto.getItemName());
        entity.setQty(dto.getQty() != null ? dto.getQty() : 1);
        entity.setUnitPrice(dto.getUnitPrice() != null ? dto.getUnitPrice() : 0L);
        entity.setAmount(dto.getAmount() != null ? dto.getAmount() : 0L);
        return entity;
    }
}
