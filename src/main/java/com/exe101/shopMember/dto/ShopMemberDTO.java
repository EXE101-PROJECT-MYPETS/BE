package com.exe101.shopMember.dto;

import com.exe101.shop.entity.ShopRole;
import com.exe101.shopMember.entity.MemberStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ShopMemberDTO {

    private Long shopId;
    private Long userId;
    private ShopRole role;
    private MemberStatus status;
    private OffsetDateTime createdAt;
    private String userFullName;
    private String userEmail;
    private String avatarUrlPreview;
}