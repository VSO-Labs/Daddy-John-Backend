package com.vso.DaddyJohn.Exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.PAYMENT_REQUIRED)
public class TokenLimitExceededException extends RuntimeException {

    public TokenLimitExceededException(String message) {
        super(message);
    }

    public TokenLimitExceededException(String message, Throwable cause) {
        super(message, cause);
    }
}