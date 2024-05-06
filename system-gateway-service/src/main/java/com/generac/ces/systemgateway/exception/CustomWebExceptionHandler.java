package com.generac.ces.systemgateway.exception;

import java.util.List;
import java.util.stream.Collectors;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice
public class CustomWebExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler({BadRequestException.class})
    public ResponseEntity<Object> handleBadRequestException(BadRequestException ex) {
        ExceptionResponse error = new ExceptionResponse();
        if (ex.getLocalizedMessage() != null) {
            error.setErrorMsg(ex.getLocalizedMessage());
        } else {
            error.setErrorMsgs(ex.getErrorMsgs());
        }

        error.setErrorCode(400);
        error.setError(HttpStatus.BAD_REQUEST.getReasonPhrase());
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(GatewayTimeoutException.class)
    public ResponseEntity<Object> handleGatewayTimeoutException(GatewayTimeoutException ex) {
        ExceptionResponse error = new ExceptionResponse();
        if (ex.getLocalizedMessage() != null) {
            error.setErrorMsg(ex.getLocalizedMessage());
        } else {
            error.setErrorMsg(ex.getMessage());
        }

        error.setErrorCode(HttpStatus.GATEWAY_TIMEOUT.value());
        error.setError(HttpStatus.GATEWAY_TIMEOUT.getReasonPhrase());
        return new ResponseEntity<>(error, HttpStatus.GATEWAY_TIMEOUT);
    }

    /**
     * WebClientResponseException is thrown by the WebClient when it receives HTTP-level errors or
     * when clientResponse.createException() or clientResponse.createError() is called.
     */
    @ExceptionHandler(WebClientResponseException.class)
    protected ResponseEntity<Object> handleFailedCallsToOtherRestApis(
            WebClientResponseException ex, ServletWebRequest request) {
        logger.error(ex.getMessage(), ex);

        ExceptionResponse error = new ExceptionResponse();
        error.setErrorCode(ex.getRawStatusCode());
        error.setErrorMsg(ex.getResponseBodyAsString());
        return new ResponseEntity<>(error, ex.getStatusCode());
    }

    @ExceptionHandler({SubscriptionBadRequestException.class})
    public ResponseEntity<Object> handleSubscriptionBadRequestException(
            SubscriptionBadRequestException ex) {
        return new ResponseEntity<>(ex.getBody(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler({SubscriptionInternalServerException.class})
    public ResponseEntity<Object> handleSubscriptionBadRequestException(
            SubscriptionInternalServerException ex) {
        return new ResponseEntity<>(ex.getBody(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler({TooManyRequestsException.class})
    public ResponseEntity<Object> handleRateLimitExceededException(TooManyRequestsException ex) {
        ExceptionResponse error = new ExceptionResponse();
        if (ex.getLocalizedMessage() != null) {
            error.setErrorMsg(ex.getLocalizedMessage());
        } else {
            error.setErrorMsgs(ex.getErrorMsgs());
        }

        error.setErrorCode(429);
        error.setError(HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase());
        return new ResponseEntity<>(error, ex.getHeaders(), HttpStatus.TOO_MANY_REQUESTS);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatus status,
            WebRequest request) {
        ExceptionResponse error = new ExceptionResponse();
        error.setErrorCode(400);
        error.setError(HttpStatus.BAD_REQUEST.getReasonPhrase());

        // Get all errors
        List<String> errors =
                ex.getBindingResult().getFieldErrors().stream()
                        .map(
                                fieldError ->
                                        fieldError.getField()
                                                + ": "
                                                + fieldError.getDefaultMessage())
                        .collect(Collectors.toList());

        error.setErrorMsgs(errors);

        return new ResponseEntity<>(error, headers, HttpStatus.BAD_REQUEST);
    }

    @Override
    protected ResponseEntity<Object> handleBindException(
            BindException ex, HttpHeaders headers, HttpStatus status, WebRequest request) {
        ExceptionResponse error = new ExceptionResponse();
        error.setErrorCode(400);
        error.setError(HttpStatus.BAD_REQUEST.getReasonPhrase());

        // Get all errors
        List<String> errors =
                ex.getBindingResult().getFieldErrors().stream()
                        .map(DefaultMessageSourceResolvable::getDefaultMessage)
                        .collect(Collectors.toList());

        error.setErrorMsgs(errors);

        return new ResponseEntity<>(error, headers, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler({ConstraintViolationException.class})
    public ResponseEntity<Object> handleConstraintViolation(ConstraintViolationException ex) {
        ExceptionResponse error = new ExceptionResponse();
        error.setErrorCode(400);
        error.setError(HttpStatus.BAD_REQUEST.getReasonPhrase());

        // Get all errors
        List<String> errors =
                ex.getConstraintViolations().stream()
                        .map(ConstraintViolation::getMessage)
                        .collect(Collectors.toList());

        error.setErrorMsgs(errors);
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }
}
