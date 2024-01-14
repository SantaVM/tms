package org.santavm.tms.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.*;

@ControllerAdvice
public class CustomExceptionHandler extends ResponseEntityExceptionHandler {

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            @NonNull HttpHeaders headers, HttpStatusCode status, @NonNull WebRequest request){

        Map<String, String> responseBody = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            responseBody.put(fieldName, errorMessage);
        });

        return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_JSON).headers(headers).body(responseBody);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex, @Nullable Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request){

        Map<String, String> responseBody = new HashMap<>();

        // from original method
        if (request instanceof ServletWebRequest servletWebRequest) {
            HttpServletResponse response = servletWebRequest.getResponse();
            if (response != null && response.isCommitted()) {
                if (logger.isWarnEnabled()) {
                    logger.warn("Response already committed. Ignoring: " + ex);
                }
                return null;
            }
        }

        // yes
        if (ex instanceof HttpMessageNotReadableException){

                String fieldName = "message";
                String errorMessage = ex.getLocalizedMessage();
                responseBody.put(fieldName, errorMessage);
        }

        return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_JSON).headers(headers).body(responseBody);

    }

}
