package com.exe101.booking.mapper;

import com.exe101.booking.dto.BookingPetDTO;
import com.exe101.booking.entity.BookingPet;
import com.exe101.booking.entity.BookingPetId;
import org.springframework.stereotype.Component;

@Component
public class BookingPetMapper {

    public static BookingPetDTO toDTO(BookingPet entity) {
        if (entity == null) return null;
        return new BookingPetDTO(entity.getBookingId(), entity.getPetId());
    }

    public static BookingPet toEntity(BookingPetDTO dto) {
        if (dto == null) return null;
        BookingPet entity = new BookingPet();
        entity.setId(new BookingPetId(dto.getBookingId(), dto.getPetId()));
        return entity;
    }
}
