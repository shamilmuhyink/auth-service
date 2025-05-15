package com.authservice.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    
    private int status;
    private String error;
    private String message;
    private String path;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;
    
    private String traceId;
    
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<ValidationError> validationErrors;
    
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Map<String, Object> details;
    
    /**
     * Create a basic error response
     */
    public static ErrorResponse of(HttpStatus status, String message, String path) {
        return ErrorResponse.builder()
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path(path)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    /**
     * Create an error response with additional details
     */
    public static ErrorResponse of(HttpStatus status, String message, String path, Map<String, Object> details) {
        ErrorResponse errorResponse = of(status, message, path);
        errorResponse.setDetails(details);
        return errorResponse;
    }
    
    /**
     * Add a validation error to this error response
     */
    public ErrorResponse addValidationError(String field, String message) {
        if (validationErrors == null) {
            validationErrors = new ArrayList<>();
        }
        validationErrors.add(new ValidationError(field, message));
        return this;
    }
    
    /**
     * Add multiple validation errors from a map
     */
    public ErrorResponse addValidationErrors(Map<String, String> errors) {
        errors.forEach(this::addValidationError);
        return this;
    }
    
    /**
     * Inner class for validation errors
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ValidationError {
        private String field;
        private String message;
    }
}