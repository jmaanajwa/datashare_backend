package com.datashare.exception;

import com.datashare.model.ApiError;
import com.datashare.model.FieldError;
import jakarta.validation.ConstraintViolationException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.ErrorResponse;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    ResponseEntity<ApiError> business(BusinessException ex) { return error(ex.getStatus(), ex.getMessage()); }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> validation(MethodArgumentNotValidException ex) {
        var body = body(HttpStatus.BAD_REQUEST, "Donnees invalides.");
        ex.getBindingResult().getFieldErrors().forEach(field -> body.addFieldErrorsItem(
                new FieldError().field(field.getField()).message(field.getDefaultMessage())));
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, ServletRequestBindingException.class,
            MethodArgumentTypeMismatchException.class, ConstraintViolationException.class,
            MissingServletRequestPartException.class})
    ResponseEntity<ApiError> invalidRequest(Exception ex) {
        return error(HttpStatus.BAD_REQUEST, "Requete invalide ou parametre manquant.");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<ApiError> tooLarge(MaxUploadSizeExceededException ex) {
        return error(HttpStatus.PAYLOAD_TOO_LARGE, "Fichier trop volumineux.");
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> unexpected(Exception ex) {
        if (ex instanceof ErrorResponse response) {
            return ResponseEntity.status(response.getStatusCode()).body(new ApiError()
                    .code(response.getStatusCode().value()).message("Requete non prise en charge.")
                    .timestamp(OffsetDateTime.now(ZoneOffset.UTC)));
        }
        log.error("Erreur interne pendant le traitement de la requete", ex);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Une erreur interne est survenue.");
    }

    private static ResponseEntity<ApiError> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(body(status, message));
    }

    private static ApiError body(HttpStatus status, String message) {
        return new ApiError().code(status.value()).message(message).timestamp(OffsetDateTime.now(ZoneOffset.UTC));
    }
}
