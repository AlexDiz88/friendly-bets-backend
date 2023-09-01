package net.friendly_bets.exceptions;

import org.springframework.http.HttpStatus;

public class RestException extends RuntimeException {
    private final HttpStatus httpStatus;

    public RestException(HttpStatus httpStatus, String message) {
        super(message);
        this.httpStatus = httpStatus;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
