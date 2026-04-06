package com.exe101.customer.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CustomerDTO {
    private Long id;
    private Long shopId;
    private Long userId;
    private String fullName;
    private String phone;
    private String email;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
