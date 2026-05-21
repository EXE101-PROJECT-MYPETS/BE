package com.exe101.ai.dto;

import lombok.*;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiAction {
    private String type;
    private String toolName;
    private Map<String, Object> arguments;
    private List<String> missingFields;
}
