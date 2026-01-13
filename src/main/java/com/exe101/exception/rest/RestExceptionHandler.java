package com.exe101.exception.rest;

import com.exe101.exception.AppException;
import com.exe101.exception.NotFoundException;
import com.exe101.exception.PermissionNotAllowedException;
import com.exe101.exception.ValidateException;
import com.exe101.exception.payload.ErrorPayload;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
@RestController
public class RestExceptionHandler {

    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<ErrorPayload> handleNullPointerException(
            NullPointerException e
    ) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorPayload(
                        "INTERNAL_ERROR",
                        "Unexpected error occurred",
                        null
                ));
    }

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ErrorPayload> handleAppException(AppException ex) {

        HttpStatus status;

        if (ex instanceof ValidateException) {
            status = HttpStatus.PRECONDITION_FAILED;
        } else if (ex instanceof NotFoundException) {
            status = HttpStatus.NOT_FOUND;
        } else if (ex instanceof PermissionNotAllowedException) {
            status = HttpStatus.FORBIDDEN;
        } else {
            status = HttpStatus.BAD_REQUEST;
        }

        return ResponseEntity
                .status(status)
                .body(new ErrorPayload(
                        ex.getCode(),
                        ex.getMessage() != null ? ex.getMessage() : "",
                        ex.getData()
                ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorPayload> handleValidationException(
            MethodArgumentNotValidException ex
    ) {
        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult()
                .getFieldErrors()
                .forEach(error ->
                        errors.put(error.getField(), error.getDefaultMessage())
                );

        return ResponseEntity
                .status(HttpStatus.PRECONDITION_FAILED)
                .body(new ErrorPayload(
                        "VALIDATION_ERROR",
                        "Vui lòng nhập đúng định dạng",
                        errors
                ));
    }

}