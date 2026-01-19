package ru.slie.luna.sdk.utils;

public class ParseValueException extends Exception {
    private final String message;

    public ParseValueException(String message) {
        this.message = message;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
