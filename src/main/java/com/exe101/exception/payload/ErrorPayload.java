package com.exe101.exception.payload;

import lombok.*;

@Data
@Getter
@Setter
public class ErrorPayload {
    private String code;
    private String message;
    private Object data;


    public ErrorPayload(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public ErrorPayload(String code, String message, Object data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }
}
