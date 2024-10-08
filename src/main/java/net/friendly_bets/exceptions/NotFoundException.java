package net.friendly_bets.exceptions;

import org.springframework.http.HttpStatus;

public class NotFoundException extends RestException {
    public NotFoundException(String entity, String id) {
        super(HttpStatus.NOT_FOUND, formatMessage(entity, id));
    }

    public static String formatMessage(String entity, String id) {
        return entity + " with id <" + id + "> not found.";
    }
}
