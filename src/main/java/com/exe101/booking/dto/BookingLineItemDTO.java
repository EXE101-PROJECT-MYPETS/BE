package com.exe101.booking.dto;

import com.exe101.booking.entity.BookingItemType;
import com.exe101.service_shop.entity.ServiceType;
import com.exe101.service_shop.entity.VeterinaryServiceType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BookingLineItemDTO {

    private Long bookingItemId;
    private BookingItemType itemType;
    private Long refId;
    private Long productId;
    private Long serviceId;
    private String name;
    private Long petId;
    private String petName;
    private ServiceType serviceType;
    private VeterinaryServiceType veterinaryServiceType;
    private Long vaccineId;
    private String vaccineName;
    private Integer quantity;
    private Long unitPrice;
    private Long amount;
}
