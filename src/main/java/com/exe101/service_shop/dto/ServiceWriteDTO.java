package com.exe101.service_shop.dto;

import com.exe101.service_shop.entity.ServiceType;
import com.exe101.service_shop.entity.VeterinaryServiceType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ServiceWriteDTO {

    @NotBlank(message = "TÃªn dá»‹ch vá»¥ khÃ´ng Ä‘Æ°á»£c Ä‘á»ƒ trá»‘ng")
    @Size(max = 255, message = "TÃªn dá»‹ch vá»¥ khÃ´ng Ä‘Æ°á»£c vÆ°á»£t quÃ¡ 255 kÃ½ tá»±")
    private String name;

    @NotNull(message = "Thá»i lÆ°á»£ng dá»‹ch vá»¥ khÃ´ng Ä‘Æ°á»£c Ä‘á»ƒ trá»‘ng")
    @Min(value = 1, message = "Thá»i lÆ°á»£ng dá»‹ch vá»¥ pháº£i lá»›n hÆ¡n 0")
    private Integer durationMin;

    @NotNull(message = "GiÃ¡ dá»‹ch vá»¥ khÃ´ng Ä‘Æ°á»£c Ä‘á»ƒ trá»‘ng")
    @Min(value = 0, message = "GiÃ¡ dá»‹ch vá»¥ pháº£i lá»›n hÆ¡n hoáº·c báº±ng 0")
    private Long basePrice;

    private Long categoryId;
    private ServiceType serviceType;
    private VeterinaryServiceType veterinaryServiceType;
    private Long vaccineId;

    @Size(max = 1000, message = "ÄÆ°á»ng dáº«n áº£nh khÃ´ng Ä‘Æ°á»£c vÆ°á»£t quÃ¡ 1000 kÃ½ tá»±")
    private String imageUrl;

    private Boolean active;
}
