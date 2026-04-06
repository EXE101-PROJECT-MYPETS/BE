package com.exe101.booking.mapper;

import com.exe101.booking.dto.BookingItemDTO;
import com.exe101.booking.entity.BookingItem;
import org.springframework.stereotype.Component;

@Component
public class BookingItemMapper {

    public static BookingItemDTO toDTO(BookingItem entity) {
        if (entity == null) return null;
        return new BookingItemDTO(
                entity.getId(),
                entity.getShopId(),
                entity.getBookingId(),
                entity.getPetId(),
                entity.getItemType(),
                entity.getRefId(),
                entity.getQty(),
                entity.getUnitPrice(),
                entity.getAmount(),
                entity.getCreatedAt()
        );
    }

    public static BookingItem toEntity(BookingItemDTO dto) {
        if (dto == null) return null;
        BookingItem entity = new BookingItem();
        entity.setShopId(dto.getShopId());
        entity.setBookingId(dto.getBookingId());
        entity.setPetId(dto.getPetId());
        entity.setItemType(dto.getItemType());
        entity.setRefId(dto.getRefId());
        entity.setQty(dto.getQty() != null ? dto.getQty() : 1);
        entity.setUnitPrice(dto.getUnitPrice() != null ? dto.getUnitPrice() : 0L);
        entity.setAmount(dto.getAmount() != null ? dto.getAmount() : 0L);
        return entity;
    }
}
