package com.exe101.auth.util;

import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;

public final class ResponseUtil {

    private ResponseUtil() {
        // prevent instantiation
    }

    public static void writeError(
            HttpServletResponse response,
            int status,
            String message
    ) throws IOException {

        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        try (PrintWriter out = response.getWriter()) {
            out.write("{\"message\":\"" + message + "\"}");
        }
    }
}