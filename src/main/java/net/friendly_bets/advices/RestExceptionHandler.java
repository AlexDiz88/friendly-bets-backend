package net.friendly_bets.advices;

import lombok.extern.slf4j.Slf4j;
import net.friendly_bets.dto.StandardResponseDto;
import net.friendly_bets.exceptions.BadDataException;
import net.friendly_bets.exceptions.ConflictException;
import net.friendly_bets.exceptions.ForbiddenException;
import net.friendly_bets.exceptions.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@Slf4j
@ControllerAdvice
public class RestExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<StandardResponseDto> handleNotFound(NotFoundException ex) {
        log.error(ex.toString());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(StandardResponseDto.builder()
                        .message(ex.getMessage())
                        .status(HttpStatus.NOT_FOUND.value())
                        .build());
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<StandardResponseDto> handleForbidden(ForbiddenException ex) {
        log.error(ex.toString());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(StandardResponseDto.builder()
                        .message(ex.getMessage())
                        .status(HttpStatus.FORBIDDEN.value())
                        .build());
    }

    @ExceptionHandler(BadDataException.class)
    public ResponseEntity<StandardResponseDto> handleBadData(BadDataException ex) {
        log.error(ex.toString());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(StandardResponseDto.builder()
                        .message(ex.getMessage())
                        .status(HttpStatus.BAD_REQUEST.value())
                        .build());
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<StandardResponseDto> handleConflict(ConflictException ex) {
        log.error(ex.toString());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(StandardResponseDto.builder()
                        .message(ex.getMessage())
                        .status(HttpStatus.CONFLICT.value())
                        .build());
    }
}
