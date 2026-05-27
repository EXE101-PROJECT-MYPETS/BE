package com.exe101.order.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderCancelRequestReviewDTO {
    @Size(max = 1000, message = "Ghi chú xử lý yêu cầu hủy không được vượt quá 1000 ký tự")
    private String reviewNote;
}
