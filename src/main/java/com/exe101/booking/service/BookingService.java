package com.exe101.booking.service;

import com.exe101.booking.dto.BookingDTO;
import com.exe101.booking.entity.Booking;
import com.exe101.booking.exception.BookingNotFound;
import com.exe101.booking.mapper.BookingMapper;
import com.exe101.booking.repository.IBookingRepository;
import com.exe101.common.IService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BookingService implements IService<Booking, BookingDTO, Long> {

    private final IBookingRepository bookingRepository;

    @Override
    public List<BookingDTO> getAll() {
        return bookingRepository.findAll().stream().map(BookingMapper::toDTO).toList();
    }

    @Override
    public BookingDTO getById(Long id) {
        return bookingRepository.findById(id)
                .map(BookingMapper::toDTO)
                .orElseThrow(() -> new BookingNotFound("BookingNotFound", "Booking not found"));
    }

    @Override
    public BookingDTO create(BookingDTO dto) {
        return BookingMapper.toDTO(bookingRepository.save(BookingMapper.toEntity(dto)));
    }

    @Override
    public BookingDTO update(Long id, BookingDTO dto) {
        Booking entity = bookingRepository.findById(id)
                .orElseThrow(() -> new BookingNotFound("BookingNotFound", "Booking not found"));
        BookingMapper.updateEntity(entity, dto);
        return BookingMapper.toDTO(bookingRepository.save(entity));
    }

    @Override
    public void delete(Long id) {
        if (!bookingRepository.existsById(id)) {
            throw new BookingNotFound("BookingNotFound", "Booking not found");
        }
        bookingRepository.deleteById(id);
    }
}
