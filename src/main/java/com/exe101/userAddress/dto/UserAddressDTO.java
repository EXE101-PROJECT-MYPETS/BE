package com.exe101.userAddress.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserAddressDTO {
    private Long id;
    private Long userId;
    private String name;
    private String tel;
    private String address;
    private String province;
    private String district;
    private String ward;
    private String hamlet;

    @JsonProperty("isDefault")
    private Boolean defaultAddress;

    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
