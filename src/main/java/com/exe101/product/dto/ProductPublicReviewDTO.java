package com.exe101.product.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductPublicReviewDTO {
    private Long id;
    private Integer star;
    private String content;
    private List<String> imageUrls;
    private ProductPublicReviewUserDTO user;
    private OffsetDateTime date;
    private Long usefulCount;
}
