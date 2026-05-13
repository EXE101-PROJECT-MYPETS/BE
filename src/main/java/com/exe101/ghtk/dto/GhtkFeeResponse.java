package com.exe101.ghtk.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GhtkFeeResponse {

    private boolean success;
    private String message;
    @JsonProperty("log_id")
    private String logId;
    private JsonNode fee;
    private JsonNode error;
    private JsonNode warning;
}
