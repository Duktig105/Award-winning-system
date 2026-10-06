USE awardsystem;

DELIMITER $$
DROP PROCEDURE IF EXISTS add_team_upgrade_column$$
CREATE PROCEDURE add_team_upgrade_column(IN table_value VARCHAR(64), IN column_value VARCHAR(64), IN definition_value VARCHAR(500))
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name=table_value AND column_name=column_value) THEN
    SET @team_upgrade_ddl=CONCAT('ALTER TABLE `',table_value,'` ADD COLUMN `',column_value,'` ',definition_value);
    PREPARE team_upgrade_statement FROM @team_upgrade_ddl;
    EXECUTE team_upgrade_statement;
    DEALLOCATE PREPARE team_upgrade_statement;
  END IF;
END$$
DELIMITER ;

CALL add_team_upgrade_column('student_team_profile','show_bio','TINYINT(1) NOT NULL DEFAULT 0');
CALL add_team_upgrade_column('student_team_profile','show_portfolio','TINYINT(1) NOT NULL DEFAULT 0');
CALL add_team_upgrade_column('student_team_profile','show_weekly_hours','TINYINT(1) NOT NULL DEFAULT 0');
CALL add_team_upgrade_column('team_member','position_id','INT NULL');
DROP PROCEDURE add_team_upgrade_column;

CREATE TABLE IF NOT EXISTS team_recommendation_weights (
  config_id INT NOT NULL PRIMARY KEY,
  skill_weight INT NOT NULL,
  role_weight INT NOT NULL,
  time_weight INT NOT NULL,
  experience_weight INT NOT NULL,
  background_weight INT NOT NULL,
  version INT NOT NULL DEFAULT 1,
  updated_by INT NULL,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_team_weights_user FOREIGN KEY (updated_by) REFERENCES `user`(user_id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS team_recommendation_weights_history (
  history_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  version INT NOT NULL,
  skill_weight INT NOT NULL,
  role_weight INT NOT NULL,
  time_weight INT NOT NULL,
  experience_weight INT NOT NULL,
  background_weight INT NOT NULL,
  operator_user_id INT NOT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_team_weights_history_user FOREIGN KEY (operator_user_id) REFERENCES `user`(user_id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT IGNORE INTO team_recommendation_weights(config_id,skill_weight,role_weight,time_weight,experience_weight,background_weight,version)
VALUES(1,40,20,15,15,10,1);
