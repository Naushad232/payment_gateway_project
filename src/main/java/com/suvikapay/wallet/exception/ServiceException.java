// src/main/java/com/suvikapay/wallet/exception/ServiceException.java
package com.suvikapay.wallet.exception;

public class ServiceException extends RuntimeException {

    public ServiceException(String message) {
        super(message);
    }

    public ServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}