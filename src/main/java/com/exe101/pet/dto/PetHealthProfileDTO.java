package com.exe101.pet.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PetHealthProfileDTO {
    private Long petId;
    private String allergies;
    private String conditions;
    private String notes;
    private OffsetDateTime updatedAt;
}
