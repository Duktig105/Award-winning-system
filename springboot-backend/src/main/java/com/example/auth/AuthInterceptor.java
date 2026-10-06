package com.example.auth;

import com.example.common.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;

@Component
public class AuthInterceptor implements HandlerInterceptor {
    private final JwtService jwtService;
    private final ObjectMapper mapper;

    public AuthInterceptor(JwtService jwtService, ObjectMapper mapper) {
        this.jwtService = jwtService;
        this.mapper = mapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) return true;
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) return reject(response, 401, "AUTH_REQUIRED", "请先登录");
        try {
            AuthUser user = jwtService.verify(header.substring(7));
            AuthContext.set(user);
            String path = request.getRequestURI();
            if (path.startsWith("/api/admin/") && !user.hasRole("admin")) {
                return reject(response, 403, "FORBIDDEN", "需要管理员权限");
            }
            if ((path.startsWith("/api/review") || path.startsWith("/api/statistics") || path.startsWith("/api/competition-manage"))
                    && !(user.hasRole("admin") || user.hasRole("mentor"))) {
                return reject(response, 403, "FORBIDDEN", "无权访问管理功能");
            }
            if (path.startsWith("/api/student/") && requiresStudentManagementRole(path) && !user.hasRole("admin")) {
                return reject(response, 403, "FORBIDDEN", "需要管理员权限");
            }
            if ((path.equals("/application/all") || path.equals("/application/updateStatus"))
                    && !(user.hasRole("admin") || user.hasRole("mentor"))) {
                return reject(response, 403, "FORBIDDEN", "无权访问审核功能");
            }
            return true;
        } catch (IllegalArgumentException e) {
            return reject(response, 401, "TOKEN_INVALID", e.getMessage());
        }
    }

    private boolean requiresStudentManagementRole(String path) {
        return path.startsWith("/api/student/list")
                || path.startsWith("/api/student/add")
                || path.startsWith("/api/student/update")
                || path.startsWith("/api/student/delete")
                || path.startsWith("/api/student/batch-delete")
                || path.startsWith("/api/student/export")
                || path.startsWith("/api/student/import")
                || path.startsWith("/api/student/reset-password")
                || path.matches("/api/student/\\d+");
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        AuthContext.clear();
    }

    private boolean reject(HttpServletResponse response, int status, String code, String message) throws Exception {
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(mapper.writeValueAsString(Result.error(code, message)));
        return false;
    }
}
