package com.example.controller;

import com.example.common.Result;
import com.example.entity.User;
import com.example.service.UserService;
import com.example.auth.AuthContext;
import com.example.auth.JwtService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;
import java.util.LinkedHashMap;
import java.util.Map;
/**
 * 用户控制器
 * 处理与用户相关的所有HTTP请求，包括登录、修改密码等功能
 */
@RestController
@RequestMapping("/user")
public class UserController {

    /**
     * 注入用户服务层对象
     * 通过@Resource注解自动装配UserService实例
     */
    @Resource
    UserService userService;

    @Resource
    JwtService jwtService;

    /**
     * 用户登录接口
     * 处理POST请求，验证用户凭据并返回登录结果
     * @param user 用户实体对象，包含用户名和密码
     * @return Result 统一响应结果，成功时返回用户信息
     */
    @PostMapping("/login")
    public Result login(@RequestBody User user) {
        User dbUser = userService.login(user);
        String token = jwtService.issue(dbUser);
        dbUser.setPassword(null);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("token", token);
        payload.put("user", dbUser);
        return Result.success(payload);
    }

    /**
     * 修改密码接口
     * 处理POST请求，用于修改用户密码
     * @param request 包含用户名、旧密码、新密码的请求对象
     * @return Result 统一响应结果，成功时返回200状态码
     */
    @PostMapping("/changePassword")
    public Result changePassword(@RequestBody java.util.Map<String, String> request) {
        try {
            String username = AuthContext.require().username();
            String oldPassword = request.get("oldPassword");
            String newPassword = request.get("newPassword");
            
            userService.changePassword(username, oldPassword, newPassword);
            System.out.println("密码修改成功: " + username);
            return Result.success();
        } catch (Exception e) {
            System.err.println("修改密码失败: " + e.getMessage());
            e.printStackTrace();
            return Result.error("修改密码失败: " + e.getMessage());
        }
    }

    /**
     * 用户登出接口
     * 处理POST请求，用于用户退出登录
     * @return Result 统一响应结果
     */
    @PostMapping("/logout")
    public Result logout() {
        try {
            System.out.println("用户退出登录");
            // 前端会清除本地存储的用户信息和token
            // 后端这里可以记录登出日志或清除session（如果使用session的话）
            return Result.success("退出登录成功");
        } catch (Exception e) {
            System.err.println("退出登录失败: " + e.getMessage());
            return Result.error("退出登录失败: " + e.getMessage());
        }
    }
}
