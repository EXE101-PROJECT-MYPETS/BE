package com.exe101.pet.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PetDTO {
    private Long id;
    private Long shopId;
    private Long customerId;
    private Long speciesId;
    private Long breedId;
    private String breedText;
    private String avatarUrl;
    private String name;
    private String gender;
    private LocalDate dob;
    private String note;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
