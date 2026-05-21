package com.exe101.shop.controller;

import com.exe101.shop.dto.ShopDTO;
import com.exe101.shop.dto.ShopProfileUpdateRequest;
import com.exe101.shop.service.ShopService;
import com.exe101.shopMember.dto.ShopMemberDTO;
import com.exe101.shopMember.service.ShopMemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/shops")
@RequiredArgsConstructor
public class ShopController {

    private final ShopService shopService;
    private final ShopMemberService shopMemberService;

    @GetMapping("/owner/profile")
    public ResponseEntity<ShopDTO> getOwnerProfile(@RequestHeader("X-Shop-Id") Long shopId) {
        return ResponseEntity.ok(shopService.getById(shopId));
    }

    @PutMapping(value = "/owner/profile", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ShopDTO> updateOwnerProfile(
            @RequestHeader("X-Shop-Id") Long shopId,
            @Valid @RequestBody ShopDTO dto
    ) {
        return ResponseEntity.ok(shopService.update(shopId, dto));
    }

    @PutMapping(value = "/owner/profile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ShopDTO> updateOwnerProfileMultipart(
            @RequestHeader("X-Shop-Id") Long shopId,
            @ModelAttribute @Valid ShopProfileUpdateRequest request
    ) {
        return ResponseEntity.ok(shopService.update(shopId, request));
    }

    @GetMapping("/staff")
    public ResponseEntity<List<ShopMemberDTO>> getStaff(@RequestHeader("X-Shop-Id") Long shopId) {
        return ResponseEntity.ok(shopMemberService.getActiveStaffByShop(shopId));
    }
}
