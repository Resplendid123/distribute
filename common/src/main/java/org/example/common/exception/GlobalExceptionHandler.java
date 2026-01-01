package org.example.common.exception;

import org.example.common.context.Result;
import org.example.common.enums.ResultCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleException(Exception e) {
        log.error("Unexpected error", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.fail(ResultCode.INTERNAL_SERVER_ERROR));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Result<Void>> handleIllegalArgumentException(IllegalArgumentException e) {
        log.error("Illegal argument", e);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Result.fail(ResultCode.BAD_REQUEST));
    }
}
