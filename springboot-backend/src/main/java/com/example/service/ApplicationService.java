package com.example.service;

import com.example.dto.ApplicationSubmitDTO;
import com.example.entity.AwardApplication;
import com.example.entity.Competition;
import com.example.mapper.ApplicationMapper;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.example.team.TeamService;

/**
 * 申请业务逻辑层
 * 处理申请相关业务，调用Mapper层操作数据库
 */
@Service
public class ApplicationService {

    /**
     * 注入申请Mapper，通过Mapper操作数据库
     */
    @Resource
    ApplicationMapper applicationMapper;

    @Resource
    TeamService teamService;

    // 从配置文件读取文件上传路径
    @Value("${file.upload.path}")
    private String uploadPath;

    /**
     * 提交申请
     * 
     * @param dto       申请数据
     * @param files     证明材料文件
     * @param studentId 当前登录学生ID
     * @return 申请ID
     */
    @Transactional
    public Integer submitApplication(ApplicationSubmitDTO dto, MultipartFile[] files, Integer studentId)
            throws Exception {
        // 1. 验证竞赛ID是否存在
        Integer competitionId = dto.getCompetitionId();
        if (competitionId == null) {
            throw new Exception("请选择竞赛");
        }

        Map<String, Object> selectedTeam = null;
        if (Boolean.TRUE.equals(dto.getIsTeam()) && dto.getTeamId() != null) {
            selectedTeam = teamService.lockedTeamForApplication(dto.getTeamId());
            if (!java.util.Objects.equals(selectedTeam.get("competitionId"), competitionId)) {
                throw new Exception("所选队伍与竞赛不匹配");
            }
        }

        // 2. 创建申请对象
        AwardApplication application = new AwardApplication();
        application.setCompetitionId(competitionId);
        application.setStudentId(studentId);
        application.setProjectName(dto.getProjectName());
        application.setTeamName(selectedTeam != null ? String.valueOf(selectedTeam.get("name")) : dto.getTeamName());
        application.setCompetitionLevel(dto.getCompetitionLevel());
        application.setAwardRank(dto.getAwardLevel()); // 获奖等次（A/B/C/D）
        application.setAwardLevel(dto.getAwardRank()); // 获奖等级（金奖/一等奖等）

        // 解析获奖时间
        if (dto.getAwardTime() != null && !dto.getAwardTime().isEmpty()) {
            application.setAwardTime(LocalDate.parse(dto.getAwardTime()));
        }

        application.setContact(dto.getContact());

        // 设置获奖数量和获奖人数（个人赛默认为1，团体赛根据团队成员数量设置）
        if (selectedTeam != null) {
            application.setAwardQuantity(1);
            application.setAwardPersonCount(((List<?>) selectedTeam.get("members")).size());
        } else if (Boolean.TRUE.equals(dto.getIsTeam()) && dto.getTeamMembers() != null) {
            application.setAwardQuantity(1); // 获奖数量默认为1
            application.setAwardPersonCount(dto.getTeamMembers().size()); // 获奖人数为团队成员数
        } else {
            application.setAwardQuantity(1); // 个人赛获奖数量为1
            application.setAwardPersonCount(1); // 个人赛获奖人数为1
        }

        // 指导老师信息已移至 application_teacher 表，此处不再设置

        // 设置团队ID（如果是团体赛，需要先创建团队记录，这里暂时设为null）
        application.setTeamId(selectedTeam != null ? dto.getTeamId() : null);

        // 设置申请状态和时间
        application.setApplicationStatus("pending");
        application.setCreateTime(LocalDateTime.now());

        // 生成申请编号
        application.setApplicationNumber(generateApplicationNumber());

        System.out.println("========== 准备插入申请数据 ==========");
        System.out.println("竞赛ID: " + application.getCompetitionId());
        System.out.println("学生ID: " + application.getStudentId());
        System.out.println("项目名称: " + application.getProjectName());
        System.out.println("申请编号: " + application.getApplicationNumber());

        // 3. 保存申请到数据库
        int result = applicationMapper.insert(application);
        System.out.println("数据库插入结果: " + result + " 行");
        System.out.println("生成的申请ID: " + application.getApplicationId());

        if (application.getApplicationId() == null) {
            throw new Exception("申请ID生成失败，数据库插入可能失败");
        }

        // 4. 保存文件（如果有）
        if (files != null && files.length > 0) {
            System.out.println("开始保存 " + files.length + " 个文件");
            saveFiles(files, application.getApplicationId());
        }

        // 5. 保存指导老师信息到 application_teacher 表
        if (dto.getTeachers() != null && !dto.getTeachers().isEmpty()) {
            System.out.println("开始保存 " + dto.getTeachers().size() + " 位指导老师");
            for (int i = 0; i < dto.getTeachers().size(); i++) {
                ApplicationSubmitDTO.TeacherDTO teacherDTO = dto.getTeachers().get(i);

                com.example.entity.ApplicationTeacher applicationTeacher = new com.example.entity.ApplicationTeacher();
                applicationTeacher.setApplicationId(application.getApplicationId());
                applicationTeacher.setTeacherName(teacherDTO.getTeacherName());
                applicationTeacher.setTeacherDepartment(teacherDTO.getTeacherDepartment());
                applicationTeacher.setTeacherNo(teacherDTO.getTeacherNo());

                applicationMapper.insertApplicationTeacher(applicationTeacher);
                System.out.println("保存指导老师: " + teacherDTO.getTeacherName() + ", ID: " + applicationTeacher.getId());
            }
        }

        // 6. 保存团队成员（如果是团体赛）
        if (selectedTeam == null && Boolean.TRUE.equals(dto.getIsTeam()) && dto.getTeamMembers() != null && !dto.getTeamMembers().isEmpty()) {
            System.out.println("开始保存 " + dto.getTeamMembers().size() + " 位团队成员");

            // 先创建团队记录
            com.example.entity.Team team = new com.example.entity.Team();
            team.setName(dto.getTeamName() != null ? dto.getTeamName() : "未命名团队");
            team.setLeaderId(null); // 暂不设置队长
            team.setCompetitionId(competitionId);
            team.setHasSort(dto.getConfirmMemberOrder() != null && dto.getConfirmMemberOrder() ? 1 : 0);
            team.setCreateTime(LocalDateTime.now());

            applicationMapper.insertTeam(team);
            Integer teamId = team.getTeamId();
            System.out.println("创建团队: " + team.getName() + ", 团队ID: " + teamId);

            // 保存团队成员（使用已创建的 team_id）
            for (int i = 0; i < dto.getTeamMembers().size(); i++) {
                ApplicationSubmitDTO.TeamMemberDTO memberDTO = dto.getTeamMembers().get(i);

                com.example.entity.TeamMember teamMember = new com.example.entity.TeamMember();
                teamMember.setTeamId(teamId);

                if (memberDTO.getIsExternal()) {
                    // 校外成员
                    teamMember.setStudentId(null);
                    teamMember.setExternalName(memberDTO.getExternalName());
                    teamMember.setExternalNumber(memberDTO.getExternalNumber());
                    teamMember.setExternalSchool(memberDTO.getExternalSchool());
                } else {
                    // 校内成员 - 根据学号查询student_id
                    com.example.entity.Student student = applicationMapper
                            .selectStudentByNumber(memberDTO.getStudentNumber());
                    if (student != null) {
                        teamMember.setStudentId(student.getStudentId());
                    } else {
                        teamMember.setStudentId(null);
                    }
                    teamMember.setExternalName(memberDTO.getStudentName());
                    teamMember.setExternalNumber(memberDTO.getStudentNumber());
                    teamMember.setExternalSchool(null);
                }

                teamMember.setIsLeader(memberDTO.getIsLeader() != null && memberDTO.getIsLeader() ? 1 : 0);
                teamMember.setSortOrder(i + 1);

                applicationMapper.insertTeamMember(teamMember);
                System.out.println("保存团队成员: " + teamMember.getExternalName() + ", ID: " + teamMember.getTeamMemberId());
            }

            // 更新申请表的 team_id
            applicationMapper.updateApplicationTeamId(application.getApplicationId(), teamId);
            System.out.println("更新申请表的 team_id 完成");
        }

        System.out.println("========== 申请提交完成 ==========");

        return application.getApplicationId();
    }

    /**
     * 根据竞赛名称查找竞赛
     */
    private Integer findCompetition(String competitionName) throws Exception {
        // 搜索是否存在该竞赛
        List<Competition> competitions = applicationMapper.searchCompetitionByName(competitionName);

        if (competitions == null || competitions.isEmpty()) {
            throw new Exception("竞赛不存在，请从列表中选择正确的竞赛名称");
        }

        // 返回已有的竞赛ID
        return competitions.get(0).getCompetitionId();
    }

    // ==================== 竞赛相关业务 ====================

    /**
     * 根据关键词搜索竞赛
     */
    public List<Competition> searchCompetitions(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return applicationMapper.selectAllCompetitions();
        }
        return applicationMapper.searchCompetitionByName(keyword);
    }

    /**
     * 获取所有竞赛
     */
    public List<Competition> getAllCompetitions() {
        return applicationMapper.selectAllCompetitions();
    }

    // ==================== 申请查询业务 ====================

    /**
     * 根据学生ID查询申请列表（包含竞赛名称）
     */
    public List<Map<String, Object>> getApplicationsByStudentId(Integer studentId) {
        List<AwardApplication> applications = applicationMapper.selectByStudentId(studentId);

        // 转换为 Map 并添加竞赛名称
        return applications.stream().map(app -> {
            Map<String, Object> map = new java.util.HashMap<>();
            map.put("applicationId", app.getApplicationId());
            map.put("competitionId", app.getCompetitionId());
            map.put("teamId", app.getTeamId());
            map.put("studentId", app.getStudentId());
            map.put("projectName", app.getProjectName());
            map.put("awardQuantity", app.getAwardQuantity());
            map.put("awardPersonCount", app.getAwardPersonCount());
            map.put("contact", app.getContact());
            map.put("competitionLevel", app.getCompetitionLevel());
            map.put("awardRank", app.getAwardRank());
            map.put("awardLevel", app.getAwardLevel());
            map.put("awardTime", app.getAwardTime());
            map.put("applicationStatus", app.getApplicationStatus());
            map.put("createTime", app.getCreateTime());
            map.put("teamName", app.getTeamName());
            map.put("applicationNumber", app.getApplicationNumber());

            // 根据 competitionId 查询竞赛名称
            if (app.getCompetitionId() != null) {
                Competition competition = applicationMapper.selectCompetitionById(app.getCompetitionId());
                if (competition != null) {
                    map.put("competitionName", competition.getCompetitionName());
                }
            }

            return map;
        }).collect(java.util.stream.Collectors.toList());
    }

    /**
     * 根据申请ID查询申请详情（包含指导老师、团队成员、证明材料）
     */
    public Map<String, Object> getApplicationDetail(Integer applicationId) {
        Map<String, Object> result = new java.util.HashMap<>();

        // 查询申请基本信息
        AwardApplication application = applicationMapper.selectById(applicationId);
        if (application == null) {
            return result;
        }

        // 将申请信息转为Map
        result.put("applicationId", application.getApplicationId());
        result.put("competitionId", application.getCompetitionId());
        result.put("teamId", application.getTeamId());
        result.put("studentId", application.getStudentId());
        result.put("projectName", application.getProjectName());
        result.put("awardQuantity", application.getAwardQuantity());
        result.put("awardPersonCount", application.getAwardPersonCount());
        result.put("contact", application.getContact());
        result.put("competitionLevel", application.getCompetitionLevel());
        result.put("awardRank", application.getAwardRank());
        result.put("awardLevel", application.getAwardLevel());
        result.put("awardTime", application.getAwardTime());
        result.put("applicationStatus", application.getApplicationStatus());
        result.put("createTime", application.getCreateTime());
        result.put("teamName", application.getTeamName());
        result.put("applicationNumber", application.getApplicationNumber());

        // 查询竞赛信息
        if (application.getCompetitionId() != null) {
            Competition competition = applicationMapper.selectCompetitionById(application.getCompetitionId());
            if (competition != null) {
                result.put("competitionName", competition.getCompetitionName());
                result.put("competitionType", competition.getCompetitionType());
            }
        }

        // 查询指导老师
        List<Map<String, Object>> teachers = applicationMapper.selectTeachersByApplicationId(applicationId);
        result.put("teachers", teachers != null ? teachers : new java.util.ArrayList<>());

        // 查询团队成员（如果是团体赛）
        if (application.getTeamId() != null) {
            List<Map<String, Object>> teamMembers = applicationMapper
                    .selectTeamMembersByTeamId(application.getTeamId());
            result.put("teamMembers", teamMembers != null ? teamMembers : new java.util.ArrayList<>());
        } else {
            result.put("teamMembers", new java.util.ArrayList<>());
        }

        // 查询证明材料
        List<Map<String, Object>> files = applicationMapper.selectFilesByApplicationId(applicationId);
        result.put("files", files != null ? files : new java.util.ArrayList<>());

        return result;
    }

    /**
     * 查询所有申请
     */
    public List<AwardApplication> getAllApplications() {
        return applicationMapper.selectAll();
    }

    /**
     * 更新申请状态
     */
    public void updateApplicationStatus(Integer applicationId, String status) {
        applicationMapper.updateStatus(applicationId, status);
    }

    /**
     * 撤回申请（仅待审核状态可撤回）
     */
    public void withdrawApplication(Integer applicationId, Integer studentId) throws Exception {
        // 查询申请信息
        AwardApplication application = applicationMapper.selectById(applicationId);

        if (application == null) {
            throw new Exception("申请不存在");
        }

        // 验证是否是申请人本人
        if (!application.getStudentId().equals(studentId)) {
            throw new Exception("无权限撤回此申请");
        }

        // 只有待审核状态才能撤回
        if (!"pending".equals(application.getApplicationStatus())) {
            throw new Exception("只有待审核状态的申请才能撤回");
        }

        // 删除申请记录
        applicationMapper.deleteById(applicationId);
    }

    /**
     * 生成申请编号
     */
    private String generateApplicationNumber() {
        // 格式：AP + 年月日 + 4位随机数（如AP202602100123）
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String random = String.format("%04d", (int) (Math.random() * 10000));
        return "AP" + date + random;
    }

    /**
     * 保存文件到服务器并记录到数据库
     */
    private void saveFiles(MultipartFile[] files, Integer applicationId) throws IOException {
        // 创建上传目录（使用配置文件中的路径）
        String fileDir = uploadPath + applicationId + "/";
        File dir = new File(fileDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        // 保存每个文件
        for (MultipartFile file : files) {
            if (!file.isEmpty()) {
                String originalFilename = file.getOriginalFilename();
                String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
                String newFilename = UUID.randomUUID().toString() + extension;

                // 保存文件到磁盘
                Path filePath = Paths.get(fileDir + newFilename);
                Files.write(filePath, file.getBytes());

                // 保存文件信息到数据库
                com.example.entity.ApplicationFile applicationFile = new com.example.entity.ApplicationFile();
                applicationFile.setApplicationId(applicationId);
                applicationFile.setFileName(originalFilename);

                // 【核心修复】：数据库只保存相对网络映射路径，而非本机的 C盘/D盘 绝对物理路径
                String networkPath = "/uploads/applications/" + applicationId + "/" + newFilename;
                applicationFile.setFilePath(networkPath);

                applicationFile.setFileType(extension);
                applicationFile.setFileSize((int) file.getSize());
                applicationFile.setUploadTime(LocalDateTime.now());

                applicationMapper.insertFile(applicationFile);

                System.out.println("文件已保存到磁盘: " + filePath.toAbsolutePath());
                System.out.println("文件信息已保存到数据库，文件ID: " + applicationFile.getFileId());
            }
        }
    }
}
