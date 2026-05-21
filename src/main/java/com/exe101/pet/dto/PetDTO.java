package com.exe101.pet.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PetDTO {
    private Long id;
    private Long userId;
    private Long speciesId;
    private Long breedId;
    private String breedText;
    private String avatarUrl;
    private String name;
    private String gender;
    private LocalDate dob;
    private BigDecimal weightKg;
    private String note;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
