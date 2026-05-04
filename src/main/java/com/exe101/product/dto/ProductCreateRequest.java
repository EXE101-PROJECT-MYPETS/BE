package com.exe101.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class ProductCreateRequest {

    private Long categoryId;

    @NotBlank(message = "SKU không được để trống")
    @Size(max = 64, message = "SKU không được vượt quá 64 ký tự")
    private String sku;

    @NotBlank(message = "Tên sản phẩm không được để trống")
    @Size(max = 255, message = "Tên sản phẩm không được vượt quá 255 ký tự")
    private String name;

    @Size(max = 50, message = "Đơn vị không được vượt quá 50 ký tự")
    private String unit;

    @PositiveOrZero(message = "Giá phải lớn hơn hoặc bằng 0")
    private Long price;

    @PositiveOrZero(message = "Khối lượng phải lớn hơn hoặc bằng 0")
    private BigDecimal weightKg;

    private Boolean active;

    @PositiveOrZero(message = "Số lượng tồn phải lớn hơn hoặc bằng 0")
    private Long stockQty;

    private List<@NotBlank(message = "Đường dẫn ảnh không được để trống") @Size(max = 1000, message = "Đường dẫn ảnh không được vượt quá 1000 ký tự") String> imageUrls;

    private Boolean replaceImages;

    private List<MultipartFile> imageFiles;
}
