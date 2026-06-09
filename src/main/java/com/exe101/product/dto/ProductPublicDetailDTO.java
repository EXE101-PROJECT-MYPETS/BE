package com.exe101.product.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductPublicDetailDTO {
    private Long id;
    private String name;
    private Long price;
    private BigDecimal weightKg;
    private Long priceSale;
    private ProductPublicCategoryDTO category;
    private Long shopId;
    private String shopName;
    private String shopLogoUrl;
    private Boolean shopVerified;
    private Double shopRating;
    private Long shopProductCount;
    private String shopAddress;
    private String shopContactName;
    private String shopContactPhone;
    private String shopContactEmail;
    private List<String> imageUrls;
    private Double reviewAvg;
    private Long reviewCount;
    private Long soldCount;
    private Long stockQty;
    private String unit;
    private List<String> variants;
    private List<String> attributes;
    private List<String> policies;
}
