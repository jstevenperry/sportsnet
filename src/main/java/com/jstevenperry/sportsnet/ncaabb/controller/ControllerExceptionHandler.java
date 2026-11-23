/*
 * Copyright © 2021 J. Steven Perry
 * All rights reserved.
 */

package com.jstevenperry.sportsnet.ncaabb.controller;

import com.jstevenperry.sportsnet.framework.exception.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class ControllerExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ControllerExceptionHandler.class);

    @ExceptionHandler(value = BadRequestException.class)
    public ResponseEntity<?> handleBadRequest(final BadRequestException exception) {
        String errorMessage = String.format("Bad request: %s", exception.getLocalizedMessage());
        log.error("handleBadRequest(): {}", errorMessage, exception);
        return ResponseEntity.badRequest().body(errorMessage);
    }

    @ExceptionHandler(value = Exception.class)
    public ResponseEntity<?> handleCatchAll(final Exception exception) {
        String errorMessage = String.format("Exception: %s", exception.getLocalizedMessage());
        log.error("handleCatchAll(): {}", errorMessage, exception);
        return ResponseEntity.internalServerError().body(errorMessage);
    }
}
