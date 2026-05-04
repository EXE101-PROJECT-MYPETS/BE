package com.exe101.shopMember.dto;

import com.exe101.shop.entity.ShopRole;
import com.exe101.shopMember.entity.MemberStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ShopMemberUpdateRequest {

    private ShopRole role;

    private MemberStatus status;
}
