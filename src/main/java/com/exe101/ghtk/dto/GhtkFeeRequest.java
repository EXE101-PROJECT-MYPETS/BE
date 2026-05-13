package com.exe101.ghtk.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GhtkFeeRequest {

    @NotNull(message = "userAddressId không được để trống")
    private Long userAddressId;

    @NotNull(message = "Khối lượng không được để trống")
    @Min(value = 1, message = "Khối lượng phải lớn hơn 0")
    private Long weight;

    @NotNull(message = "Giá trị đơn hàng không được để trống")
    @Min(value = 0, message = "Giá trị đơn hàng không được âm")
    private Long value;

    @NotNull(message = "Phương thức vận chuyển không được để trống")
    @Pattern(regexp = "road|fly", message = "Phương thức vận chuyển chỉ được là road hoặc fly")
    private String transport;
}
