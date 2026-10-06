USE awardsystem;

DELIMITER $$
DROP PROCEDURE IF EXISTS add_saims_column$$
CREATE PROCEDURE add_saims_column(IN table_name_value VARCHAR(64), IN column_name_value VARCHAR(64), IN definition_value VARCHAR(500))
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = table_name_value AND column_name = column_name_value
  ) THEN
    SET @ddl = CONCAT('ALTER TABLE `', table_name_value, '` ADD COLUMN `', column_name_value, '` ', definition_value);
    PREPARE statement_value FROM @ddl;
    EXECUTE statement_value;
    DEALLOCATE PREPARE statement_value;
  END IF;
END$$
DELIMITER ;

CALL add_saims_column('competition','competition_type',"VARCHAR(20) DEFAULT '团体赛'");
CALL add_saims_column('competition','min_team_size','INT NOT NULL DEFAULT 1');
CALL add_saims_column('competition','max_team_size','INT NOT NULL DEFAULT 10');
CALL add_saims_column('competition','team_open_time','DATETIME NULL');
CALL add_saims_column('competition','team_close_time','DATETIME NULL');
CALL add_saims_column('competition','team_enabled','TINYINT(1) NOT NULL DEFAULT 1');
CALL add_saims_column('team','description','VARCHAR(1000) NULL');
CALL add_saims_column('team','status',"VARCHAR(20) NOT NULL DEFAULT 'recruiting'");
CALL add_saims_column('team','recruiting','TINYINT(1) NOT NULL DEFAULT 1');
CALL add_saims_column('team','target_size','INT NOT NULL DEFAULT 2');
CALL add_saims_column('team','version','INT NOT NULL DEFAULT 0');
CALL add_saims_column('team','update_time','DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP');
CALL add_saims_column('team','locked_time','DATETIME NULL');
CALL add_saims_column('team','removed_reason','VARCHAR(255) NULL');
CALL add_saims_column('team_member','role_name','VARCHAR(50) NULL');
CALL add_saims_column('team_member','member_status',"VARCHAR(20) NOT NULL DEFAULT 'accepted'");
CALL add_saims_column('team_member','join_time','DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP');
ALTER TABLE `user` MODIFY COLUMN `password` VARCHAR(255) NOT NULL;
DROP PROCEDURE add_saims_column;

CREATE TABLE IF NOT EXISTS student_team_profile (
  profile_id INT NOT NULL AUTO_INCREMENT,
  student_id INT NOT NULL,
  bio VARCHAR(500) NULL,
  specialties VARCHAR(500) NULL,
  competition_experience VARCHAR(1500) NULL,
  portfolio_url VARCHAR(500) NULL,
  weekly_hours INT NOT NULL DEFAULT 0,
  preferred_roles VARCHAR(500) NULL,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (profile_id),
  UNIQUE KEY uk_team_profile_student (student_id),
  CONSTRAINT fk_team_profile_student FOREIGN KEY (student_id) REFERENCES student(student_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS skill_tag (
  skill_id INT NOT NULL AUTO_INCREMENT,
  name VARCHAR(40) NOT NULL,
  PRIMARY KEY (skill_id),
  UNIQUE KEY uk_skill_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS student_skill (
  student_id INT NOT NULL,
  skill_id INT NOT NULL,
  PRIMARY KEY (student_id, skill_id),
  CONSTRAINT fk_student_skill_student FOREIGN KEY (student_id) REFERENCES student(student_id) ON DELETE CASCADE,
  CONSTRAINT fk_student_skill_skill FOREIGN KEY (skill_id) REFERENCES skill_tag(skill_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS team_position (
  position_id INT NOT NULL AUTO_INCREMENT,
  team_id INT NOT NULL,
  title VARCHAR(50) NOT NULL,
  vacancies INT NOT NULL DEFAULT 1,
  requirements VARCHAR(500) NULL,
  required_skills VARCHAR(500) NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'open',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (position_id),
  KEY idx_position_team_status (team_id, status),
  CONSTRAINT fk_position_team FOREIGN KEY (team_id) REFERENCES team(team_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS team_request (
  request_id INT NOT NULL AUTO_INCREMENT,
  team_id INT NOT NULL,
  student_id INT NOT NULL,
  position_id INT NULL,
  request_type VARCHAR(20) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'pending',
  message VARCHAR(500) NULL,
  operator_id INT NOT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (request_id),
  KEY idx_request_student_status (student_id, status),
  KEY idx_request_team_status (team_id, status),
  CONSTRAINT fk_request_team FOREIGN KEY (team_id) REFERENCES team(team_id) ON DELETE CASCADE,
  CONSTRAINT fk_request_student FOREIGN KEY (student_id) REFERENCES student(student_id) ON DELETE CASCADE,
  CONSTRAINT fk_request_position FOREIGN KEY (position_id) REFERENCES team_position(position_id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS system_notification (
  notification_id INT NOT NULL AUTO_INCREMENT,
  user_id INT NOT NULL,
  type VARCHAR(30) NOT NULL,
  title VARCHAR(100) NOT NULL,
  content VARCHAR(500) NOT NULL,
  link VARCHAR(255) NULL,
  is_read TINYINT(1) NOT NULL DEFAULT 0,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (notification_id),
  KEY idx_notification_user_read (user_id, is_read, create_time),
  CONSTRAINT fk_notification_user FOREIGN KEY (user_id) REFERENCES `user`(user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS team_report (
  report_id INT NOT NULL AUTO_INCREMENT,
  team_id INT NOT NULL,
  reporter_user_id INT NOT NULL,
  reason VARCHAR(500) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'pending',
  resolution VARCHAR(500) NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  resolved_time DATETIME NULL,
  PRIMARY KEY (report_id),
  KEY idx_report_status (status, create_time),
  CONSTRAINT fk_report_team FOREIGN KEY (team_id) REFERENCES team(team_id) ON DELETE CASCADE,
  CONSTRAINT fk_report_user FOREIGN KEY (reporter_user_id) REFERENCES `user`(user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS team_audit_log (
  audit_id BIGINT NOT NULL AUTO_INCREMENT,
  operator_user_id INT NOT NULL,
  team_id INT NULL,
  action VARCHAR(50) NOT NULL,
  detail VARCHAR(1000) NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (audit_id),
  KEY idx_audit_team_time (team_id, create_time),
  CONSTRAINT fk_audit_user FOREIGN KEY (operator_user_id) REFERENCES `user`(user_id) ON DELETE RESTRICT,
  CONSTRAINT fk_audit_team FOREIGN KEY (team_id) REFERENCES team(team_id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
