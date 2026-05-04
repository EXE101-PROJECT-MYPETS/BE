package com.exe101.shopMember.controller;

import com.exe101.shop.entity.ShopRole;
import com.exe101.shopMember.dto.ShopMemberCreateRequest;
import com.exe101.shopMember.dto.ShopMemberDTO;
import com.exe101.shopMember.dto.ShopMemberResetPasswordRequest;
import com.exe101.shopMember.dto.ShopMemberUpdateRequest;
import com.exe101.shopMember.entity.MemberStatus;
import com.exe101.shopMember.service.ShopMemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/shop-members")
@RequiredArgsConstructor
public class ShopMemberController {

    private final ShopMemberService shopMemberService;

    @GetMapping
    public ResponseEntity<List<ShopMemberDTO>> getAll(
            @RequestHeader("X-Shop-Id") Long shopId,
            @RequestParam(required = false) ShopRole role,
            @RequestParam(required = false) MemberStatus status,
            @RequestParam(required = false) String keyword
    ) {
        return ResponseEntity.ok(shopMemberService.getAllByShop(shopId, role, status, keyword));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<ShopMemberDTO> getById(
            @RequestHeader("X-Shop-Id") Long shopId,
            @PathVariable Long userId
    ) {
        return ResponseEntity.ok(shopMemberService.getByShopAndUserId(shopId, userId));
    }

    @PostMapping
    public ResponseEntity<ShopMemberDTO> create(
            @RequestHeader("X-Shop-Id") Long shopId,
            @Valid @RequestBody ShopMemberCreateRequest request
    ) {
        return ResponseEntity.ok(shopMemberService.create(shopId, request));
    }

    @PatchMapping("/{userId}")
    public ResponseEntity<ShopMemberDTO> update(
            @RequestHeader("X-Shop-Id") Long shopId,
            @PathVariable Long userId,
            @RequestBody ShopMemberUpdateRequest request
    ) {
        return ResponseEntity.ok(shopMemberService.update(shopId, userId, request));
    }

    @PatchMapping("/{userId}/password")
    public ResponseEntity<Void> resetPassword(
            @RequestHeader("X-Shop-Id") Long shopId,
            @PathVariable Long userId,
            @Valid @RequestBody ShopMemberResetPasswordRequest request
    ) {
        shopMemberService.resetPassword(shopId, userId, request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> delete(
            @RequestHeader("X-Shop-Id") Long shopId,
            @PathVariable Long userId
    ) {
        shopMemberService.delete(shopId, userId);
        return ResponseEntity.noContent().build();
    }
}
