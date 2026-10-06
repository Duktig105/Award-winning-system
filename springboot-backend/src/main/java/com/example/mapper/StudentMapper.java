package com.example.mapper;

import com.example.entity.Student;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface StudentMapper {

        /**
         * 查询学生总数
         */
        int countStudents(@Param("studentNumber") String studentNumber,
                        @Param("grade") String grade,
                        @Param("className") String className);

        /**
         * 分页查询学生列表
         */
        List<Student> selectStudentList(
                        @Param("studentNumber") String studentNumber,
                        @Param("grade") String grade,
                        @Param("className") String className,
                        @Param("offset") Integer offset,
                        @Param("pageSize") Integer pageSize);

        /**
         * 根据ID查询学生
         */
        Student selectStudentById(@Param("studentId") Integer studentId);

        /**
         * 插入学生
         */
        int insertStudent(Student student);

        /**
         * 更新学生
         */
        int updateStudent(Student student);

        /**
         * 删除学生
         */
        int deleteStudent(@Param("studentId") Integer studentId);

        /**
         * 批量删除学生
         */
        int batchDeleteStudents(@Param("ids") List<Integer> ids);

        /**
         * 查询所有学生（用于导出）
         */
        List<Student> selectAllStudents(@Param("studentNumber") String studentNumber,
                        @Param("grade") String grade,
                        @Param("className") String className);

        /**
         * 查询所有班级
         */
        List<String> selectDistinctClasses();

        /**
         * 更新学生密码
         */
        int updatePassword(@Param("studentId") Integer studentId,
                        @Param("password") String password);

        /**
         * 查询各年级学生人数统计
         */
        List<java.util.Map<String, Object>> countStudentsByGrade();
}
