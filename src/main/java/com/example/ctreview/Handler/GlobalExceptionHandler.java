package com.example.ctreview.Handler;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.NoSuchElementException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({NoSuchElementException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, Object> handleNotFound(Exception e) {
        return Map.of("error", "NOT_FOUND", "message", e.getMessage());
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class, MethodArgumentNotValidException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleBadRequest(Exception e) {
        String msg = e instanceof MethodArgumentNotValidException manve ?
                (manve.getBindingResult().getFieldError() != null ? manve.getBindingResult().getFieldError().getDefaultMessage() : "잘못된 요청")
                : e.getMessage();
        return Map.of("error", "BAD_REQUEST", "message", msg);
    }
}
