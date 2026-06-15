package com.exe101.order.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderCancelRequestCreateDTO {
    @Size(max = 1000, message = "Lý do hủy đơn không được vượt quá 1000 ký tự")
    private String reason;
}
