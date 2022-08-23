package com.delta.standalone.lib.Exception;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.io.IOException;
import java.text.ParseException;
import java.util.Objects;

@ControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    public static final String TRACE = "trace";

    @Value("${reflectoring.trace:false}")
    private boolean printStackTrace;


    @ExceptionHandler(ItemNotFoundException.class)
    @ResponseBody
    public ResponseEntity<ErrorResponse> handleItemNotFoundException(
            ItemNotFoundException itemNotFoundException,
            WebRequest request
    ){
        return buildErrorResponse(itemNotFoundException, HttpStatus.NOT_FOUND, request);
    }

    @ExceptionHandler(IOException.class)
    @ResponseBody
    public ResponseEntity<ErrorResponse> handleIOException(
            Exception exception,
            WebRequest request){
        return buildErrorResponse(
                exception,
                "Unable to read application.properties file to load key-vault configuration",
                HttpStatus.INTERNAL_SERVER_ERROR,
                request
        );
    }

    @ExceptionHandler(ParseException.class)
    @ResponseBody
    public ResponseEntity<ErrorResponse> handleParseException(
            Exception exception,
            WebRequest request){
        return buildErrorResponse(
                exception,
                "Unable to Parse stats while reading file metadata",
                HttpStatus.INTERNAL_SERVER_ERROR,
                request
        );
    }

    private ResponseEntity<ErrorResponse> buildErrorResponse(
            Exception exception,
            HttpStatus httpStatus,
            WebRequest request
    ) {
        return buildErrorResponse(
                exception,
                exception.getMessage(),
                httpStatus,
                request);
    }

    private ResponseEntity<ErrorResponse> buildErrorResponse(
            Exception exception,
            String message,
            HttpStatus httpStatus,
            WebRequest request
    ) {
        ErrorResponse errorResponse = new ErrorResponse(
                httpStatus.value(),message
        );

        if(printStackTrace && isTraceOn(request)){
            errorResponse.setStackTrace(ExceptionUtils.getStackTrace(exception));
        }

        return ResponseEntity.status(httpStatus).body(errorResponse);
    }

    private boolean isTraceOn(WebRequest request) {
        String [] value = request.getParameterValues(TRACE);
        return Objects.nonNull(value)
                && value.length > 0
                && value[0].contentEquals("true");
    }
}
