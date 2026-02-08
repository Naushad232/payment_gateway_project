// src/main/java/com/suvikapay/wallet/exception/UnauthorizedException.java
package com.suvikapay.wallet.exception;

public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}