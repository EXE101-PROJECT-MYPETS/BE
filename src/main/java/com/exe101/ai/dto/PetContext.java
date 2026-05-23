package com.exe101.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PetContext {
    private Long petId;
    private String name;
    private String species;
    private String speciesName;
    private String breed;
    private String age;
    private String weight;
    private String gender;
    private String generalNote;
    private String allergies;
    private String conditions;
    private String healthNotes;
    private String vaccinationSummary;
}
