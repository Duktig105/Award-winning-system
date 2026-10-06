-- ============================================================
-- 演示账号（全部为虚构数据，仅供本地联调使用）
-- 导入顺序：schema.sql -> sample-data.sql -> demo-accounts.sql
-- ============================================================
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 虚构学生信息（不对应任何真实学生）
INSERT INTO `student` (`student_id`, `student_number`, `student_name`, `grade`, `major`, `class_name`, `college`) VALUES
(100001, '202600010001', '演示学生一', '25级', '生物医学工程',   '生医2501班',   '生物医学工程学院'),
(100002, '202600010002', '演示学生二', '25级', '医学信息工程', '医学信息2501班', '生物医学工程学院');

-- 登录账号（密码为明文演示值，与账号相同；生产环境必须改造为加密存储）
INSERT INTO `user` (`user_id`, `username`, `password`, `role`, `student_id`, `mentor_id`, `status`) VALUES
(100001, '202600010001', '202600010001', 'student', 100001, NULL, 'enabled'),
(100002, '202600010002', '202600010002', 'student', 100002, NULL, 'enabled'),
(100003, 'admin',        'admin123',    'admin',   NULL,   NULL, 'enabled');

SET FOREIGN_KEY_CHECKS = 1;
