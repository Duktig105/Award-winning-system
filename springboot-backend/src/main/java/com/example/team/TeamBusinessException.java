package com.example.team;

public class TeamBusinessException extends RuntimeException {
    private final String code;
    public TeamBusinessException(String code, String message) {
        super(message);
        this.code = code;
    }
    public String getCode() { return code; }
}
