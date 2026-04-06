package com.exe101.booking.mapper;

import com.exe101.booking.dto.BookingResourceDTO;
import com.exe101.booking.entity.BookingResource;
import com.exe101.booking.entity.BookingResourceId;
import org.springframework.stereotype.Component;

@Component
public class BookingResourceMapper {

    public static BookingResourceDTO toDTO(BookingResource entity) {
        if (entity == null) return null;
        return new BookingResourceDTO(entity.getBookingId(), entity.getResourceId());
    }

    public static BookingResource toEntity(BookingResourceDTO dto) {
        if (dto == null) return null;
        BookingResource entity = new BookingResource();
        entity.setId(new BookingResourceId(dto.getBookingId(), dto.getResourceId()));
        return entity;
    }
}
