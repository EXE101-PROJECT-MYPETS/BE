package com.exe101.servicePackage.mapper;

import com.exe101.servicePackage.dto.PackageLedgerDTO;
import com.exe101.servicePackage.entity.PackageLedger;
import org.springframework.stereotype.Component;

@Component
public class PackageLedgerMapper {

    public static PackageLedgerDTO toDTO(PackageLedger entity) {
        if (entity == null) return null;
        return new PackageLedgerDTO(
                entity.getId(),
                entity.getShopId(),
                entity.getCustomerPackageId(),
                entity.getBookingId(),
                entity.getDeltaUses(),
                entity.getDeltaAmount(),
                entity.getReason(),
                entity.getCreatedAt()
        );
    }

    public static PackageLedger toEntity(PackageLedgerDTO dto) {
        if (dto == null) return null;
        PackageLedger entity = new PackageLedger();
        entity.setShopId(dto.getShopId());
        entity.setCustomerPackageId(dto.getCustomerPackageId());
        entity.setBookingId(dto.getBookingId());
        entity.setDeltaUses(dto.getDeltaUses());
        entity.setDeltaAmount(dto.getDeltaAmount() != null ? dto.getDeltaAmount() : 0L);
        entity.setReason(dto.getReason());
        return entity;
    }
}
