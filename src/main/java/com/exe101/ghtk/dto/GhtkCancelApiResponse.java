package com.exe101.ghtk.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GhtkCancelApiResponse {
    private boolean success;
    private String message;

    @JsonProperty("log_id")
    private String logId;
}
