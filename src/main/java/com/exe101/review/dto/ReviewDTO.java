package com.exe101.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReviewDTO {
    private Long id;
    private Long shopId;
    @NotNull(message = "San pham khong duoc de trong")
    private Long productId;
    @NotNull(message = "Khach hang khong duoc de trong")
    private Long customerId;
    private String customerName;

    @NotNull(message = "Danh gia khong duoc de trong")
    @Min(value = 1, message = "So sao phai tu 1 den 5")
    @Max(value = 5, message = "So sao phai tu 1 den 5")
    private Integer rating;

    @Size(max = 2000, message = "Noi dung danh gia khong duoc vuot qua 2000 ky tu")
    private String comment;

    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
