package com.exe101.subscription.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SepayIpnResponse {
    private boolean success;
    private String message;
}
