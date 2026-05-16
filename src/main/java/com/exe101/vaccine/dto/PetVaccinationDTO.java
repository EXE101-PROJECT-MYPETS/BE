package com.exe101.vaccine.dto;

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
public class PetVaccinationDTO {
    private Long id;
    private Long petId;
    private Long vaccineId;
    private LocalDate vaccinatedAt;
    private LocalDate nextDueAt;
    private String clinicName;
    private String vetName;
    private String batchNo;
    private String note;
    private Long createdBy;
    private OffsetDateTime createdAt;
}
