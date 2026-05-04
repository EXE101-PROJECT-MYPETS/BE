package com.exe101.ghtk.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GhtkSubmitOrderResponse {

    private boolean success;
    private String message;
    private JsonNode order;
    private JsonNode error;
    private JsonNode warning;
}
