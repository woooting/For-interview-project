package com.velrix.shared.web;

import com.velrix.shared.api.ApiResponse;
import com.velrix.shared.exception.BizException;
import com.velrix.shared.exception.ForbiddenException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public ResponseEntity<ApiResponse<Void>> handleBizException(BizException e) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT)
                .body(ApiResponse.fail(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiResponse<Void>> handleForbiddenException(ForbiddenException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.fail(e.getCode(), e.getMessage()));
    }
}