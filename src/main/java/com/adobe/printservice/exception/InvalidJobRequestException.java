package com.adobe.printservice.exception;

public class InvalidJobRequestException extends RuntimeException {

    public InvalidJobRequestException(String message) {
        super(message);
    }
}
