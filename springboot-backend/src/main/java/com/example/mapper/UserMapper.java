package com.example.mapper;

import com.example.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 用户Mapper接口
 */
@Mapper
public interface UserMapper {

    /**
     * 根据用户名查询用户
     * 通过用户名查找对应的用户记录
     * @param username 用户用户名
     * @return User 用户对象，如果不存在则返回null
     */
    @Select("select * from `user` where username = #{username}")
    User selectByUsername(String username);
    
    /**
     * 根据用户ID查询用户
     */
    User selectById(@Param("userId") Integer userId);
    
    /**
     * 根据用户名更新密码
     * 通过用户名更新对应的用户密码
     * @param username 用户用户名
     * @param password 新密码
     */
    void updatePasswordByUsername(String username, String password);
    
    /**
     * 根据学生ID查询学生姓名
     */
    @Select("SELECT student_name FROM student WHERE student_id = #{studentId}")
    String selectStudentNameById(@Param("studentId") Integer studentId);
    
    /**
     * 根据导师ID查询导师姓名
     */
    @Select("SELECT mentor_name FROM mentor WHERE mentor_id = #{mentorId}")
    String selectMentorNameById(@Param("mentorId") Integer mentorId);
}
