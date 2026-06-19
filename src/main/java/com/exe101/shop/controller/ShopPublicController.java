package com.exe101.shop.controller;

import com.exe101.common.ScrollResponse;
import com.exe101.service_shop.dto.ServicePublicDTO;
import com.exe101.service_shop.service.ServicePublicService;
import com.exe101.shop.dto.ShopMarkerDTO;
import com.exe101.shop.dto.ShopNearbyDTO;
import com.exe101.shop.dto.ShopPublicDTO;
import com.exe101.shop.service.ShopPublicService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class ShopPublicController {

    private final ShopPublicService shopPublicService;
    private final ServicePublicService servicePublicService;

    @GetMapping("/shops/markers")
    public ResponseEntity<List<ShopMarkerDTO>> getAllShopMarkers() {
        return ResponseEntity.ok(shopPublicService.getAllMarkers());
    }

    @GetMapping("/shops/nearby")
    public ResponseEntity<List<ShopNearbyDTO>> getNearbyShops(
            @RequestParam Double lat,
            @RequestParam Double lng,
            @RequestParam(required = false) Double radiusKm,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(shopPublicService.getNearbyShops(lat, lng, radiusKm, size));
    }

    @GetMapping("/shops/{shopId}")
    public ResponseEntity<ShopPublicDTO> getShopById(@PathVariable Long shopId) {
        return ResponseEntity.ok(shopPublicService.getById(shopId));
    }

    @GetMapping("/shops/{shopId}/services")
    public ResponseEntity<ScrollResponse<ServicePublicDTO>> getShopServices(
            @PathVariable Long shopId,
            @RequestParam(defaultValue = "true") Boolean active,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(servicePublicService.getAllByShopForQuickBooking(shopId, active, cursor, size));
    }

    @GetMapping("/shop/{shopId}")
    public ResponseEntity<ShopPublicDTO> getShopByIdAlias(@PathVariable Long shopId) {
        return ResponseEntity.ok(shopPublicService.getById(shopId));
    }
}
