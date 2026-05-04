package com.exe101.shopMember.mapper;

import com.exe101.shopMember.dto.ShopMemberDTO;
import com.exe101.shopMember.entity.MemberStatus;
import com.exe101.shopMember.entity.ShopMember;
import com.exe101.shopMember.entity.ShopMemberId;
import org.springframework.stereotype.Component;

@Component
public class ShopMemberMapper {
    public static ShopMemberDTO toDTO(ShopMember entity) {
        if (entity == null) return null;

        ShopMemberDTO dto = new ShopMemberDTO();
        dto.setShopId(entity.getShopId());
        dto.setUserId(entity.getUserId());
        dto.setRole(entity.getRole());
        dto.setStatus(entity.getStatus());
        dto.setCreatedAt(entity.getCreatedAt());
        if (entity.getUser() != null) {
            dto.setUserFullName(entity.getUser().getFullName());
            dto.setUserEmail(entity.getUser().getEmail());
            dto.setUserPhone(entity.getUser().getPhone());
            dto.setAvatarUrlPreview(entity.getUser().getAvatarUrlPreview());
        }
        return dto;
    }

    public static ShopMember toEntity(ShopMemberDTO dto) {
        if (dto == null) return null;

        ShopMember entity = new ShopMember();
        entity.setId(new ShopMemberId(dto.getShopId(), dto.getUserId()));
        entity.setRole(dto.getRole());
        entity.setStatus(
                dto.getStatus() != null ? dto.getStatus() : MemberStatus.ACTIVE
        );
        return entity;
    }
}
