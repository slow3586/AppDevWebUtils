package ru.blogic.muzedodevwebutils.config;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice
@Slf4j
public class RestExceptionHandler extends ResponseEntityExceptionHandler {
    @ExceptionHandler(value = {RuntimeException.class, Exception.class})
    protected ResponseEntity<Object> handleConflict(
        final RuntimeException ex,
        final WebRequest request
    ) {
        val text = "Ошибка: " + ex.toString() + ": " + ex.getMessage();
        log.error("Ошибка от запроса REST: {}", ex.getMessage(), ex);
        return handleExceptionInternal(
            ex,
            text,
            new HttpHeaders(),
            HttpStatus.INTERNAL_SERVER_ERROR,
            request);
    }
}
