package com.authservice.exception;

import lombok.Getter;

/**
 * Base exception class for all application exceptions with enhanced error details
 */
@Getter
public abstract class ApiException extends RuntimeException {
    
    private final String errorCode;
    
    public ApiException(String message) {
        super(message);
        this.errorCode = null;
    }
    
    public ApiException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = null;
    }
    
    public ApiException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public ApiException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
}