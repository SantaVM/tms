package org.santavm.tms.controller;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotNull;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
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
import java.util.stream.Collectors;

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

        return new ResponseEntity<>(responseBody, headers, HttpStatus.BAD_REQUEST); // or status
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

        // no
        if (ex instanceof JwtException){

            String fieldName = "message";
            String errorMessage = ex.getLocalizedMessage();
            responseBody.put(fieldName, errorMessage);
        }

        return new ResponseEntity<>(responseBody, headers, HttpStatus.BAD_REQUEST); // or status

    }

    //TODO exceptions from jwt
}
