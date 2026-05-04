package com.exe101.ghtk.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class GhtkSubmitOrderRequest {

    @NotNull(message = "Ngày lấy hàng GHTK không được để trống")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate pickDate;

    @Min(value = 0, message = "isFreeship chỉ được là 0 hoặc 1")
    @Max(value = 1, message = "isFreeship chỉ được là 0 hoặc 1")
    private Integer isFreeship;

    @Size(max = 120, message = "Ghi chú GHTK không được vượt quá 120 ký tự")
    private String note;
}
