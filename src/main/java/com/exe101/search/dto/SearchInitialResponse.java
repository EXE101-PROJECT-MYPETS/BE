package com.exe101.search.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SearchInitialResponse {
    private List<String> recentKeywords;
    private List<String> suggestedKeywords;
    private List<SearchItemDTO> recommendedItems;
}

