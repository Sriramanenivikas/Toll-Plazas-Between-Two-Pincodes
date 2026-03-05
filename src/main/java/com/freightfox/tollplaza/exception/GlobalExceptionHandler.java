package com.freightfox.tollplaza.exception;

import com.freightfox.tollplaza.dto.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(SamePincodeException.class)
    public ResponseEntity<ErrorResponse> samePincodeExceptionHandler(SamePincodeException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(toErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(RouteNotFoundException.class)
    public ResponseEntity<ErrorResponse> routeNotFoundExceptionHandler(RouteNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(toErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(InvalidPincodeException.class)
    public ResponseEntity<ErrorResponse> invalidPincodeExceptionHandler(InvalidPincodeException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(toErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> methodArgumentExceptionHandler(MethodArgumentNotValidException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(toErrorResponse("Invalid source or destination pincode"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> exceptionHandler(Exception ex) {
        log.error("Unexpected error: ", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(toErrorResponse("Internal server error"));
    }

    private ErrorResponse toErrorResponse(String error) {
        return new ErrorResponse(error);
    }

}
