package com.example.mapper;

import com.example.entity.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 申请数据访问层
 * 统一使用 ApplicationMapper.xml 进行 SQL 映射
 */
@Mapper
public interface ApplicationMapper {

        // ==================== 申请相关操作 ====================

        /**
         * 插入申请记录
         * 
         * @param application 申请对象
         * @return 影响行数
         */
        int insert(AwardApplication application);

        /**
         * 根据申请ID查询申请
         * 
         * @param applicationId 申请ID
         * @return 申请对象
         */
        AwardApplication selectById(@Param("applicationId") Integer applicationId);

        /**
         * 根据学生ID查询申请列表
         * 
         * @param studentId 学生ID
         * @return 申请列表
         */
        List<AwardApplication> selectByStudentId(@Param("studentId") Integer studentId);

        /**
         * 查询所有申请
         * 
         * @return 所有申请列表
         */
        List<AwardApplication> selectAll();

        /**
         * 更新申请状态
         * 
         * @param applicationId 申请ID
         * @param status        新状态
         * @return 影响行数
         */
        int updateStatus(@Param("applicationId") Integer applicationId, @Param("status") String status);

        /**
         * 根据申请ID删除申请
         * 
         * @param applicationId 申请ID
         * @return 影响行数
         */
        int deleteById(@Param("applicationId") Integer applicationId);

        /**
         * 更新申请表的 team_id
         * 
         * @param applicationId 申请ID
         * @param teamId        团队ID
         * @return 影响行数
         */
        int updateApplicationTeamId(@Param("applicationId") Integer applicationId, @Param("teamId") Integer teamId);

        // ==================== 竞赛相关操作 ====================

        /**
         * 根据关键词搜索竞赛
         * 
         * @param keyword 搜索关键词
         * @return 竞赛列表
         */
        List<Competition> searchCompetitionByName(@Param("keyword") String keyword);

        /**
         * 查询所有竞赛
         * 
         * @return 所有竞赛列表
         */
        List<Competition> selectAllCompetitions();

        /**
         * 插入新竞赛
         * 
         * @param competition 竞赛对象
         * @return 影响行数
         */
        int insertCompetition(Competition competition);

        /**
         * 根据竞赛ID查询竞赛
         * 
         * @param competitionId 竞赛ID
         * @return 竞赛对象
         */
        Competition selectCompetitionById(@Param("competitionId") Integer competitionId);

        // ==================== 文件相关操作 ====================

        /**
         * 插入文件记录
         * 
         * @param file 文件对象
         * @return 影响行数
         */
        int insertFile(ApplicationFile file);

        /**
         * 根据申请ID查询文件列表
         * 
         * @param applicationId 申请ID
         * @return 文件列表（Map格式）
         */
        List<Map<String, Object>> selectFilesByApplicationId(@Param("applicationId") Integer applicationId);

        /**
         * 删除文件记录
         * 
         * @param fileId 文件ID
         * @return 影响行数
         */
        int deleteFile(@Param("fileId") Integer fileId);

        // ==================== 团队相关操作 ====================

        /**
         * 插入团队记录
         * 
         * @param team 团队对象
         * @return 影响行数
         */
        int insertTeam(Team team);

        /**
         * 根据团队ID查询团队
         * 
         * @param teamId 团队ID
         * @return 团队对象
         */
        Team selectTeamById(@Param("teamId") Integer teamId);

        /**
         * 更新团队信息
         * 
         * @param team 团队对象
         * @return 影响行数
         */
        int updateTeam(Team team);

        // ==================== 团队成员相关操作 ====================

        /**
         * 插入团队成员记录
         * 
         * @param teamMember 团队成员对象
         * @return 影响行数
         */
        int insertTeamMember(TeamMember teamMember);

        /**
         * 根据团队ID查询团队成员列表
         * 
         * @param teamId 团队ID
         * @return 团队成员列表（Map格式）
         */
        List<Map<String, Object>> selectTeamMembersByTeamId(@Param("teamId") Integer teamId);

        /**
         * 根据学号查询学生信息
         */
        com.example.entity.Student selectStudentByNumber(String studentNumber);

        /**
         * 删除团队成员
         * 
         * @param teamMemberId 团队成员ID
         * @return 影响行数
         */
        int deleteTeamMember(@Param("teamMemberId") Integer teamMemberId);

        // ==================== 指导老师相关操作 ====================

        /**
         * 插入申请-指导老师关联记录
         * 
         * @param applicationTeacher 关联对象
         * @return 影响行数
         */
        int insertApplicationTeacher(ApplicationTeacher applicationTeacher);

        /**
         * 根据申请ID查询指导老师列表
         * 
         * @param applicationId 申请ID
         * @return 指导老师列表（Map格式）
         */
        List<Map<String, Object>> selectTeachersByApplicationId(@Param("applicationId") Integer applicationId);

        /**
         * 删除指导老师记录
         * 
         * @param id 记录ID
         * @return 影响行数
         */
        int deleteApplicationTeacher(@Param("id") Integer id);

        // ==================== 审核相关操作 ====================

        /**
         * 统计指定状态的申请数量
         * 
         * @param status 申请状态（null表示统计全部）
         * @return 申请数量
         */
        int countApplicationsByStatus(@Param("status") String status);

        /**
         * 统计符合条件的申请数量（用于审核列表）
         * 
         * @param status          申请状态
         * @param competitionName 竞赛名称
         * @param studentNumber   学生学号
         * @return 申请数量
         */
        int countApplicationsForReview(
                        @Param("status") String status,
                        @Param("competitionName") String competitionName,
                        @Param("studentNumber") String studentNumber);

        /**
         * 查询申请列表（用于审核，包含学生和竞赛信息）
         * 
         * @param status          申请状态
         * @param competitionName 竞赛名称
         * @param studentNumber   学生学号
         * @param offset          偏移量
         * @param pageSize        每页数量
         * @return 申请列表
         */
        List<Map<String, Object>> selectApplicationsForReview(
                        @Param("status") String status,
                        @Param("competitionName") String competitionName,
                        @Param("studentNumber") String studentNumber,
                        @Param("offset") Integer offset,
                        @Param("pageSize") Integer pageSize);

        /**
         * 更新申请状态
         * 
         * @param applicationId 申请ID
         * @param status        新状态
         * @return 影响行数
         */
        int updateApplicationStatus(@Param("applicationId") Integer applicationId, @Param("status") String status);

        // ==================== 统计分析相关查询 ====================

        /** 按获奖等次统计已通过申请数量 */
        List<Map<String, Object>> countApprovedByAwardRank();

        /** 按竞赛级别统计已通过申请数量 */
        List<Map<String, Object>> countApprovedByCompetitionLevel();

        /** 按年级统计已通过申请数量（关联student表） */
        List<Map<String, Object>> countApprovedByGrade();

        /** Top10 热门竞赛（按申请次数排序） */
        List<Map<String, Object>> selectTop10Competitions();

        /** 近12个月每月申请数量趋势 */
        List<Map<String, Object>> selectMonthlyTrend();

        /** 按申请状态统计数量 */
        List<Map<String, Object>> countByAllStatus();
}
