package com.exe101.shopGhtkConfig.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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
public class ShopGhtkConfigDTO {

    private Long id;
    private Long shopId;
    private Boolean enabled;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Size(max = 5000, message = "Token API GHTK không được vượt quá 5000 ký tự")
    private String apiToken;

    private Boolean hasApiToken;

    @Size(max = 100, message = "Client source không được vượt quá 100 ký tự")
    private String clientSource;

    @NotBlank(message = "Tên người lấy hàng không được để trống")
    @Size(max = 255, message = "Tên người lấy hàng không được vượt quá 255 ký tự")
    private String pickName;

    @NotBlank(message = "Số điện thoại lấy hàng không được để trống")
    @Size(max = 20, message = "Số điện thoại lấy hàng không được vượt quá 20 ký tự")
    private String pickTel;

    @NotBlank(message = "Địa chỉ lấy hàng không được để trống")
    private String pickAddress;

    @NotBlank(message = "Tỉnh/thành lấy hàng không được để trống")
    @Size(max = 100, message = "Tỉnh/thành lấy hàng không được vượt quá 100 ký tự")
    private String pickProvince;

    @NotBlank(message = "Quận/huyện lấy hàng không được để trống")
    @Size(max = 100, message = "Quận/huyện lấy hàng không được vượt quá 100 ký tự")
    private String pickDistrict;

    @Size(max = 100, message = "Phường/xã lấy hàng không được vượt quá 100 ký tự")
    private String pickWard;

    @Pattern(regexp = "cod|post", message = "Hình thức lấy hàng chỉ được là cod hoặc post")
    private String pickOption;

    @Pattern(regexp = "road|fly", message = "Phương thức vận chuyển chỉ được là road hoặc fly")
    private String transport;

    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
