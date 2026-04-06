package com.exe101.servicePackage.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CustomerPackageDTO {
    private Long id;
    private Long shopId;
    private Long customerId;
    private Long packageId;
    private OffsetDateTime purchasedAt;
    private OffsetDateTime expiresAt;
    private String status;
    private OffsetDateTime createdAt;
}
