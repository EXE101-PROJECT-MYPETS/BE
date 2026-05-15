package com.exe101.search.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SearchHistoryRequest {

    @NotBlank(message = "keyword không được để trống")
    @Size(max = 255, message = "keyword không được vượt quá 255 ký tự")
    private String keyword;
}

