package com.exe101.serviceReview.dto;

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
public class ServiceReviewDTO {
    private Long id;
    private Long shopId;
    @NotNull(message = "Dịch vụ không được để trống")
    private Long serviceId;
    private Long customerId;
    private String customerName;

    @NotNull(message = "Đánh giá không được để trống")
    @Min(value = 1, message = "Số sao phải từ 1 đến 5")
    @Max(value = 5, message = "Số sao phải từ 1 đến 5")
    private Integer rating;

    @Size(max = 2000, message = "Nội dung đánh giá không được vượt quá 2000 ký tự")
    private String comment;

    private String reply;
    private OffsetDateTime replyAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
