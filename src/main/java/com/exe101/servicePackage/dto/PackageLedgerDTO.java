package com.exe101.servicePackage.dto;

import com.exe101.servicePackage.entity.PackageLedgerReason;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PackageLedgerDTO {
    private Long id;
    private Long shopId;
    private Long customerPackageId;
    private Long bookingId;
    private Integer deltaUses;
    private Long deltaAmount;
    private PackageLedgerReason reason;
    private OffsetDateTime createdAt;
}
