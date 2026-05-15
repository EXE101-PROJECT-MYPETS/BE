package com.exe101.service_shop.controller;

import com.exe101.common.ScrollResponse;
import com.exe101.service_shop.dto.ServicePublicDTO;
import com.exe101.service_shop.service.ServicePublicService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/services")
@RequiredArgsConstructor
public class ServicePublicController {

    private final ServicePublicService servicePublicService;

    @GetMapping
    public ResponseEntity<ScrollResponse<ServicePublicDTO>> getAll(
            @RequestParam(required = false) Long shopId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "true") Boolean active,
            @RequestParam(required = false) Double minRating,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(required = false) Double radiusKm,
            @RequestParam(defaultValue = "5") int perShopLimit,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(servicePublicService.getAllForScroll(
                shopId,
                search,
                categoryId,
                active,
                minRating,
                lat,
                lng,
                radiusKm,
                perShopLimit,
                cursor,
                size
        ));
    }
}
