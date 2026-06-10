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
    @NotNull(message = "Sản phẩm không được để trống")
    private Long productId;
    @NotNull(message = "Khách hàng không được để trống")
    private Long customerId;
    private String customerName;

    @NotNull(message = "Đánh giá không được để trống")
    @Min(value = 1, message = "So sao phai tu 1 den 5")
    @Max(value = 5, message = "So sao phai tu 1 den 5")
    private Integer rating;

    @Size(max = 2000, message = "Nội dung đánh giá không được vượt quá 2000 ký tự")
    private String comment;

    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
