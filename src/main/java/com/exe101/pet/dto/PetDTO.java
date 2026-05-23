package com.exe101.pet.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

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
    private Long userId;
    private Long speciesId;
    private Long breedId;
    private String breedText;
    private String avatarUrl;
    private String name;
    private String gender;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dob;
    private String note;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    @JsonIgnore
    private MultipartFile avatarUrlPreview;
}
