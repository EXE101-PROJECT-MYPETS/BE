package com.exe101.booking.dto;

import com.exe101.booking.entity.BookingItemType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BookingLineItemDTO {

    private BookingItemType itemType;
    private Long refId;
    private String name;
    private Integer quantity;
    private Long unitPrice;
    private Long amount;
}
