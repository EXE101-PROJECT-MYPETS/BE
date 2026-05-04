package com.exe101.customerAddress.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
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
public class CustomerAddressDTO {
    private Long id;

    @NotNull(message = "Khách hàng không được để trống")
    private Long customerId;

    @NotBlank(message = "Tên người nhận không được để trống")
    @Size(max = 255, message = "Tên người nhận không được vượt quá 255 ký tự")
    private String name;

    @NotBlank(message = "Số điện thoại người nhận không được để trống")
    @Size(max = 30, message = "Số điện thoại người nhận không được vượt quá 30 ký tự")
    private String tel;

    @NotBlank(message = "Địa chỉ giao hàng không được để trống")
    private String address;

    @NotBlank(message = "Tỉnh/thành phố không được để trống")
    @Size(max = 100, message = "Tỉnh/thành phố không được vượt quá 100 ký tự")
    private String province;

    @NotBlank(message = "Quận/huyện không được để trống")
    @Size(max = 100, message = "Quận/huyện không được vượt quá 100 ký tự")
    private String district;

    @NotBlank(message = "Phường/xã không được để trống")
    @Size(max = 100, message = "Phường/xã không được vượt quá 100 ký tự")
    private String ward;

    @NotBlank(message = "Thôn/xóm/ghi chú khu vực không được để trống")
    @Size(max = 255, message = "Thôn/xóm/ghi chú khu vực không được vượt quá 255 ký tự")
    private String hamlet;

    @JsonProperty("isDefault")
    private Boolean defaultAddress;

    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
