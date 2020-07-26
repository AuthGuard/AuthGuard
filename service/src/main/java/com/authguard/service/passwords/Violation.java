package com.authguard.service.passwords;

public class Violation {
    public enum Type {
        NOT_ENOUGH_CAPITAL_LETTERS,
        NOT_ENOUGH_SMALL_LETTERS,
        NOT_ENOUGH_DIGITS,
        NOT_ENOUGH_SPECIAL_CHARS,
        INVALID_SIZE
    }

    private final Type type;
    private final String message;

    public Violation(final Type type, final String message) {
        this.type = type;
        this.message = message;
    }

    public Type getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }
}
