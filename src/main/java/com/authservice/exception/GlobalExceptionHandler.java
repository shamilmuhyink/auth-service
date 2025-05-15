package com.authservice.exception;

import com.authservice.dto.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Handle custom BadRequestException
     */
    @ExceptionHandler(BadRequestException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleBadRequestException(BadRequestException ex, HttpServletRequest request) {
        return logAndCreateErrorResponse(ex, HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI());
    }

    /**
     * Handle custom ResourceNotFoundException
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleResourceNotFoundException(ResourceNotFoundException ex, HttpServletRequest request) {
        return logAndCreateErrorResponse(ex, HttpStatus.NOT_FOUND, ex.getMessage(), request.getRequestURI());
    }

    /**
     * Handle custom AppException
     */
    @ExceptionHandler(AppException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleAppException(AppException ex, HttpServletRequest request) {
        return logAndCreateErrorResponse(ex, HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage(), request.getRequestURI());
    }

    /**
     * Handle custom OAuth2AuthenticationProcessingException
     */
    @ExceptionHandler(OAuth2AuthenticationProcessingException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleOAuth2AuthenticationProcessingException(
            OAuth2AuthenticationProcessingException ex, HttpServletRequest request) {
        return logAndCreateErrorResponse(ex, HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI());
    }

    /**
     * Handle custom TokenRefreshException
     */
    @ExceptionHandler(TokenRefreshException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ErrorResponse handleTokenRefreshException(TokenRefreshException ex, HttpServletRequest request) {
        return logAndCreateErrorResponse(ex, HttpStatus.FORBIDDEN, ex.getMessage(), request.getRequestURI());
    }

    /**
     * Handle Spring Security authentication exceptions
     */
    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorResponse handleAuthenticationException(AuthenticationException ex, HttpServletRequest request) {
        String message = ex instanceof BadCredentialsException ? 
                "Invalid username or password" : ex.getMessage();
        return logAndCreateErrorResponse(ex, HttpStatus.UNAUTHORIZED, message, request.getRequestURI());
    }

    /**
     * Handle Spring Security access denied exceptions
     */
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ErrorResponse handleAccessDeniedException(AccessDeniedException ex, HttpServletRequest request) {
        return logAndCreateErrorResponse(ex, HttpStatus.FORBIDDEN, 
                "You don't have permission to access this resource", request.getRequestURI());
    }

    /**
     * Handle validation exceptions
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidationExceptions(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        
        ErrorResponse errorResponse = logAndCreateErrorResponse(ex, HttpStatus.BAD_REQUEST, 
                "Validation error", request.getRequestURI());
        
        // Add all field validation errors
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errorResponse.addValidationError(fieldError.getField(), fieldError.getDefaultMessage());
        }
        
        return errorResponse;
    }

    /**
     * Handle missing request parameters
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleMissingParams(
            MissingServletRequestParameterException ex, HttpServletRequest request) {
        String message = String.format("Required parameter '%s' of type '%s' is missing", 
                ex.getParameterName(), ex.getParameterType());
        return logAndCreateErrorResponse(ex, HttpStatus.BAD_REQUEST, message, request.getRequestURI());
    }

    /**
     * Handle request parameter type mismatch
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        String message = String.format("Parameter '%s' should be of type '%s'", 
                ex.getName(), ex.getRequiredType().getSimpleName());
        return logAndCreateErrorResponse(ex, HttpStatus.BAD_REQUEST, message, request.getRequestURI());
    }

    /**
     * Fallback handler for all other exceptions
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAllUncaughtException(
            Exception ex, WebRequest request, HttpServletRequest httpRequest) {
        
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        
        // Get the HTTP status if it's annotated on the exception
        ResponseStatus responseStatus = ex.getClass().getAnnotation(ResponseStatus.class);
        if (responseStatus != null) {
            status = responseStatus.value();
        }
        
        ErrorResponse errorResponse = logAndCreateErrorResponse(
                ex, status, "An unexpected error occurred", httpRequest.getRequestURI());
        
        return ResponseEntity.status(status).body(errorResponse);
    }

    /**
     * Helper method to log and create a standardized error response
     */
    private ErrorResponse logAndCreateErrorResponse(
            Exception ex, HttpStatus status, String message, String path) {
        
        // Generate a trace ID for the error
        String traceId = UUID.randomUUID().toString();
        
        // Log the error with the trace ID
        if (status.is5xxServerError()) {
            log.error("Error ID: {} - Exception: {}", traceId, message, ex);
        } else {
            log.warn("Error ID: {} - Exception: {}", traceId, message, ex);
        }
        
        // Create the error response
        ErrorResponse errorResponse = ErrorResponse.of(status, message, path);
        errorResponse.setTraceId(traceId);
        
        // Add extra details for certain exceptions
        if (ex instanceof ResourceNotFoundException resourceNotFoundException) {
            Map<String, Object> details = new HashMap<>();
            details.put("resourceName", resourceNotFoundException.getResourceName());
            details.put("fieldName", resourceNotFoundException.getFieldName());
            details.put("fieldValue", resourceNotFoundException.getFieldValue());
            errorResponse.setDetails(details);
        }
        
        return errorResponse;
    }
}