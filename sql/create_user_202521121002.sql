-- ============================================================
-- 创建学生账号：202521121002（密码同账号，首次登录自动升级为BCrypt加密）
-- 角色为纯 student，登录后仅可见四个模块：
--   申请填写 / 申请记录 / 竞赛组队 / 荣誉中心
-- 幂等脚本：可重复执行，不会产生重复数据
-- ============================================================
USE awardsystem;

-- 1. 若学生表中尚无该学号，先补一条学生记录（如已导入2025级名单则跳过）
INSERT INTO student (student_number, student_name, grade, major, class_name, college)
SELECT '202521121002', '2025级学生', '2025级', '软件工程', '', '计算机学院'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM student WHERE student_number = '202521121002');

-- 2. 若账号不存在，创建纯 student 角色账号（密码为明文，首次登录时系统会自动转为BCrypt加密存储）
INSERT INTO user (username, password, role, student_id, status)
SELECT '202521121002', '202521121002', 'student', s.student_id, 'enabled'
FROM student s
WHERE s.student_number = '202521121002'
  AND NOT EXISTS (SELECT 1 FROM user u WHERE u.username = '202521121002');

-- 3. 若账号已存在但角色混杂（如同时是admin/mentor），重置为纯student角色并确保账号可用
UPDATE user
SET role = 'student',
    status = 'enabled',
    student_id = (SELECT student_id FROM student WHERE student_number = '202521121002')
WHERE username = '202521121002'
  AND (role <> 'student' OR student_id IS NULL OR status <> 'enabled');

-- 验证
SELECT u.user_id, u.username, u.role, u.status, s.student_name, s.grade, s.major
FROM user u
LEFT JOIN student s ON u.student_id = s.student_id
WHERE u.username = '202521121002';
