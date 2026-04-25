package com.exe101.auth.dto;

import com.exe101.shop.entity.ShopRole;
import com.exe101.shop.entity.ShopStatus;
import com.exe101.shopMember.entity.MemberStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthenticatedShopDTO {
    private Long id;
    private String name;
    private String addressText;
    private ShopStatus shopStatus;
    private ShopRole memberRole;
    private MemberStatus memberStatus;
}
