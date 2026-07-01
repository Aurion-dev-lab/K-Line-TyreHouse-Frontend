package com.gui.kline.server.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Generic API response wrapper for all REST endpoints.
 * Provides consistent response structure with status, data, messages, and metadata.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {

    private boolean success;
    private int status;
    private String message;
    private T data;
    private Map<String, Object> metadata;
    private String timestamp;
    private String path;

    public ApiResponse(boolean success, int status, String message, T data) {
        this.success = success;
        this.status = status;
        this.message = message;
        this.data = data;
        this.timestamp = LocalDateTime.now().toString();
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, HttpStatus.OK.value(), "Success", data);
    }

    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(true, HttpStatus.OK.value(), message, data);
    }

    public static <T> ApiResponse<T> created(T data, String message) {
        return new ApiResponse<>(true, HttpStatus.CREATED.value(), message, data);
    }

    public static <T> ApiResponse<T> noContent(String message) {
        return new ApiResponse<>(true, HttpStatus.NO_CONTENT.value(), message, null);
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, HttpStatus.BAD_REQUEST.value(), message, null);
    }

    public static <T> ApiResponse<T> error(String message, int status) {
        return new ApiResponse<>(false, status, message, null);
    }

    public static <T> ApiResponse<T> notFound(String message) {
        return new ApiResponse<>(false, HttpStatus.NOT_FOUND.value(), message, null);
    }

    public static <T> ApiResponse<T> unauthorized(String message) {
        return new ApiResponse<>(false, HttpStatus.UNAUTHORIZED.value(), message, null);
    }

    public static <T> ApiResponse<T> forbidden(String message) {
        return new ApiResponse<>(false, HttpStatus.FORBIDDEN.value(), message, null);
    }

    public static <T> ApiResponse<T> serverError(String message) {
        return new ApiResponse<>(false, HttpStatus.INTERNAL_SERVER_ERROR.value(), message, null);
    }

    public static <T> ApiResponse<T> validationError(Map<String, String> errors) {
        return new ApiResponse<>(false, HttpStatus.BAD_REQUEST.value(), "Validation failed", null, errors, 
                LocalDateTime.now().toString(), null);
    }

    // Builder pattern for more complex responses
    public static <T> ApiResponseBuilder<T> builder() {
        return new ApiResponseBuilder<>();
    }

    public static class ApiResponseBuilder<T> {
        private boolean success = true;
        private int status = HttpStatus.OK.value();
        private String message = "Success";
        private T data;
        private Map<String, Object> metadata;
        private String path;

        public ApiResponseBuilder<T> success(boolean success) {
            this.success = success;
            return this;
        }

        public ApiResponseBuilder<T> status(int status) {
            this.status = status;
            return this;
        }

        public ApiResponseBuilder<T> message(String message) {
            this.message = message;
            return this;
        }

        public ApiResponseBuilder<T> data(T data) {
            this.data = data;
            return this;
        }

        public ApiResponseBuilder<T> metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }

        public ApiResponseBuilder<T> path(String path) {
            this.path = path;
            return this;
        }

        public ApiResponse<T> build() {
            ApiResponse<T> response = new ApiResponse<>(success, status, message, data);
            response.setMetadata(metadata);
            response.setTimestamp(LocalDateTime.now().toString());
            response.setPath(path);
            return response;
        }
    }
}