// src/main/java/com/suvikapay/wallet/exception/TokenExpiredException.java
package com.suvikapay.wallet.exception;

public class TokenExpiredException extends RuntimeException {

    public TokenExpiredException(String message) {
        super(message);
    }
}