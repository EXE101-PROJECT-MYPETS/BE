package com.exe101.shop.controller;

import com.exe101.shop.dto.ShopPublicDTO;
import com.exe101.shop.service.ShopPublicService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class ShopPublicController {

    private final ShopPublicService shopPublicService;

    @GetMapping("/shops/{shopId}")
    public ResponseEntity<ShopPublicDTO> getShopById(@PathVariable Long shopId) {
        return ResponseEntity.ok(shopPublicService.getById(shopId));
    }

    @GetMapping("/shop/{shopId}")
    public ResponseEntity<ShopPublicDTO> getShopByIdAlias(@PathVariable Long shopId) {
        return ResponseEntity.ok(shopPublicService.getById(shopId));
    }
}
