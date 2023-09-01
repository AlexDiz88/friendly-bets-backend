package net.friendly_bets.exceptions;

import org.springframework.http.HttpStatus;

public class ForbiddenException extends RestException {
    public ForbiddenException(String message) {
        super(HttpStatus.FORBIDDEN, message);
    }
}
