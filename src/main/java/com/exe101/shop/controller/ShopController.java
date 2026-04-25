package com.exe101.shop.controller;

import com.exe101.shopMember.dto.ShopMemberDTO;
import com.exe101.shopMember.service.ShopMemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/shops")
@RequiredArgsConstructor
public class ShopController {

    private final ShopMemberService shopMemberService;

    @GetMapping("/staff")
    public ResponseEntity<List<ShopMemberDTO>> getStaff(@RequestHeader("X-Shop-Id") Long shopId) {
        return ResponseEntity.ok(shopMemberService.getActiveStaffByShop(shopId));
    }
}
