/*
 * Copyright © 2021 J. Steven Perry
 * All rights reserved.
 */

package com.jstevenperry.sportsnet.framework.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class InternalServerErrorException extends RuntimeException {

    public InternalServerErrorException(final String message) {
        super(message);
    }

    public InternalServerErrorException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
