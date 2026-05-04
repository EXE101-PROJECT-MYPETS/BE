package com.exe101.exception.rest;

import com.exe101.exception.AppException;
import com.exe101.exception.DuplicateException;
import com.exe101.exception.NotFoundException;
import com.exe101.exception.PermissionNotAllowedException;
import com.exe101.exception.ValidateException;
import com.exe101.exception.payload.ErrorPayload;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
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
                        "Đã xảy ra lỗi không mong muốn",
                        null
                ));
    }

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ErrorPayload> handleAppException(AppException ex) {

        HttpStatus status;

        if (ex instanceof ValidateException) {
            status = HttpStatus.PRECONDITION_FAILED;
        } else if (ex instanceof DuplicateException) {
            status = HttpStatus.CONFLICT;
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

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorPayload> handleDataIntegrityViolationException(
            DataIntegrityViolationException ex
    ) {
        String message = ex.getMostSpecificCause() != null
                ? ex.getMostSpecificCause().getMessage()
                : ex.getMessage();

        boolean duplicate = message != null && message.contains("duplicate key value violates unique constraint");

        return ResponseEntity
                .status(duplicate ? HttpStatus.CONFLICT : HttpStatus.BAD_REQUEST)
                .body(new ErrorPayload(
                        duplicate ? "DuplicateResource" : "DataIntegrityViolation",
                        duplicate ? "Dữ liệu đã tồn tại" : "Dữ liệu không hợp lệ",
                        null
                ));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorPayload> handleIllegalArgumentException(
            IllegalArgumentException ex
    ) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorPayload(
                        "INVALID_REQUEST",
                        ex.getMessage() != null ? ex.getMessage() : "Yêu cầu không hợp lệ",
                        null
                ));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorPayload> handleIllegalStateException(
            IllegalStateException ex
    ) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorPayload(
                        "INVALID_STATE",
                        ex.getMessage() != null ? ex.getMessage() : "Trạng thái xử lý không hợp lệ",
                        null
                ));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorPayload> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException ex
    ) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorPayload(
                        "INVALID_REQUEST_BODY",
                        "Dữ liệu gửi lên không đúng định dạng",
                        null
                ));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorPayload> handleRuntimeException(
            RuntimeException ex
    ) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorPayload(
                        "RUNTIME_ERROR",
                        ex.getMessage() != null ? ex.getMessage() : "Đã xảy ra lỗi trong quá trình xử lý",
                        null
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
