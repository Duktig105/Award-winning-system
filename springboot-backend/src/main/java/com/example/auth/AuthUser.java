package com.example.auth;

public record AuthUser(Integer userId, String username, String role, Integer studentId, Integer mentorId) {
    public boolean hasRole(String expected) {
        if (role == null) return false;
        for (String value : role.split(",")) {
            if (expected.equals(value.trim())) return true;
        }
        return false;
    }
}
