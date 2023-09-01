package net.friendly_bets.validation.handler;

import net.friendly_bets.validation.dto.ValidationErrorDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class ValidationExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorDto> handleException(MethodArgumentNotValidException e) {
        FieldError error = e.getBindingResult().getAllErrors().stream()
                .filter(err -> err instanceof FieldError)
                .map(err -> (FieldError) err)
                .findFirst()
                .orElse(null);

        if (error != null) {
            ValidationErrorDto errorDto = ValidationErrorDto.builder()
                    .field(error.getField())
                    .message(error.getDefaultMessage())
                    .build();

            if (error.getRejectedValue() != null) {
                errorDto.setRejectedValue(error.getRejectedValue().toString());
            }

            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(errorDto);
        }
        return null;
    }
}