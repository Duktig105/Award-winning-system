package com.example.auth;

public final class AuthContext {
    private static final ThreadLocal<AuthUser> CURRENT = new ThreadLocal<>();
    private AuthContext() {}
    public static void set(AuthUser user) { CURRENT.set(user); }
    public static AuthUser get() { return CURRENT.get(); }
    public static AuthUser require() {
        AuthUser user = CURRENT.get();
        if (user == null) throw new IllegalStateException("未登录或登录已过期");
        return user;
    }
    public static void clear() { CURRENT.remove(); }
}
