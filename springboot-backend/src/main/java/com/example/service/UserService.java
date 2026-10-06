package com.example.service;

import com.example.entity.User;
import com.example.exception.CustomerException;
import com.example.mapper.UserMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 用户业务逻辑层
 * 处理用户相关业务，调用Mapper层操作数据库
 */
@Service
public class UserService {

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * 注入用户Mapper，通过Mapper操作数据库
     */
    @Resource
    UserMapper userMapper;

  /**
   * 用户登录验证
   * 验证用户凭据，包括用户名存在性检查和密码验证
   * @param user 用户实体对象，包含用户名和密码
   * @return User 验证成功时返回用户信息
   * @throws CustomerException 当用户名不存在或密码错误时抛出异常
   */
    public User login(User user) {
        // 验证账号是否存在
        User dbUser = userMapper.selectByUsername(user.getUsername());
        if (dbUser == null) {
            throw new CustomerException("账号不存在");
        }
        
        // 验证密码是否正确
        boolean encoded = dbUser.getPassword() != null && dbUser.getPassword().startsWith("$2");
        boolean passwordMatches = encoded
                ? passwordEncoder.matches(user.getPassword(), dbUser.getPassword())
                : dbUser.getPassword() != null && dbUser.getPassword().equals(user.getPassword());
        if (!passwordMatches) {
            throw new CustomerException("账号或密码错误");
        }

        if (!encoded) {
            userMapper.updatePasswordByUsername(dbUser.getUsername(), passwordEncoder.encode(user.getPassword()));
        }
        
        // 检查账号状态
        if (!"enabled".equals(dbUser.getStatus())) {
            throw new CustomerException("账号已被禁用，请联系管理员");
        }
        
        // 根据角色和关联ID查询真实姓名
        String realName = null;
        String role = dbUser.getRole();
        System.out.println("用户名: " + dbUser.getUsername());
        System.out.println("角色: " + role);
        System.out.println("学生ID: " + dbUser.getStudentId());
        System.out.println("导师ID: " + dbUser.getMentorId());
        
        if (role != null) {
            if (role.contains("student") && dbUser.getStudentId() != null) {
                // 学生：通过 student_id 查询姓名
                realName = userMapper.selectStudentNameById(dbUser.getStudentId());
                System.out.println("查询到的学生姓名: " + realName);
            } else if (role.contains("mentor") && dbUser.getMentorId() != null) {
                // 导师：通过 mentor_id 查询姓名
                realName = userMapper.selectMentorNameById(dbUser.getMentorId());
                System.out.println("查询到的导师姓名: " + realName);
            }
        }
        
        // 设置真实姓名到 User 对象
        if (realName != null) {
            dbUser.setName(realName);
            System.out.println("最终设置的姓名: " + realName);
        } else {
            System.out.println("警告: 未查询到真实姓名！");
        }
        
        return dbUser;
    }

  /**
   * 修改用户密码
   * 验证旧密码并更新为新密码
   * @param username 用户用户名
   * @param oldPassword 旧密码
   * @param newPassword 新密码
   * @throws CustomerException 当用户名不存在或旧密码错误时抛出异常
   */
    public void changePassword(String username, String oldPassword, String newPassword) {
        // 验证账号是否存在
        User dbUser = userMapper.selectByUsername(username);
        if (dbUser == null) {
            throw new CustomerException("账号不存在");
        }
        
        // 验证旧密码是否正确
        boolean matches = dbUser.getPassword() != null && dbUser.getPassword().startsWith("$2")
                ? passwordEncoder.matches(oldPassword, dbUser.getPassword())
                : dbUser.getPassword() != null && dbUser.getPassword().equals(oldPassword);
        if (!matches) {
            throw new CustomerException("原密码错误");
        }
        
        // 验证新密码不能为空
        if (newPassword == null || newPassword.trim().isEmpty()) {
            throw new CustomerException("新密码不能为空");
        }
        
        // 更新密码
        userMapper.updatePasswordByUsername(username, passwordEncoder.encode(newPassword));
    }
}
