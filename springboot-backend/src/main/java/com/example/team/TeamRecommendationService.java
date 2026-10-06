package com.example.team;

import com.example.auth.AuthContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class TeamRecommendationService {
    private final JdbcTemplate jdbc;
    public TeamRecommendationService(JdbcTemplate jdbc){this.jdbc=jdbc;}

    public List<Map<String,Object>> teamsForMe(){
        int studentId=studentId(); Map<String,Object> student=student(studentId);Map<String,Integer>w=weights();
        List<Map<String,Object>>teams=jdbc.queryForList("""
            SELECT t.team_id teamId,t.name,t.description,t.target_size targetSize,c.competition_id competitionId,
                   c.competition_name competitionName,c.max_team_size maxTeamSize,c.team_close_time teamCloseTime,
                   s.student_name leaderName,s.major leaderMajor,s.grade leaderGrade,
                   (SELECT COUNT(*) FROM team_member tm WHERE tm.team_id=t.team_id AND tm.member_status='accepted') memberCount
            FROM team t JOIN competition c ON c.competition_id=t.competition_id JOIN student s ON s.student_id=t.leader_id
            WHERE t.status='recruiting' AND t.recruiting=1 AND c.team_enabled=1
              AND (c.team_open_time IS NULL OR c.team_open_time<=NOW()) AND (c.team_close_time IS NULL OR c.team_close_time>NOW())
              AND NOT EXISTS(SELECT 1 FROM team_member tm WHERE tm.team_id=t.team_id AND tm.student_id=? AND tm.member_status='accepted')
              AND NOT EXISTS(SELECT 1 FROM team_request tr WHERE tr.team_id=t.team_id AND tr.student_id=? AND tr.status='pending')
            """,studentId,studentId);
        List<Map<String,Object>>results=new ArrayList<>();
        for(Map<String,Object>team:teams){
            int limit=Math.min(number(team.get("targetSize")),number(team.get("maxTeamSize")));
            if(number(team.get("memberCount"))>=limit || teamCount(studentId,number(team.get("competitionId")))>=2)continue;
            List<Map<String,Object>>positions=jdbc.queryForList("SELECT position_id positionId,title,required_skills requiredSkills,vacancies FROM team_position WHERE team_id=? AND status='open'",team.get("teamId"));
            for(Map<String,Object>position:positions){
                if(occupied(number(position.get("positionId")))>=number(position.get("vacancies")))continue;
                Map<String,Object>match=new LinkedHashMap<>(team);
                match.put("positionId",position.get("positionId"));match.put("positionTitle",position.get("title"));
                match.putAll(score(student,team,position,w,true));results.add(match);
            }
        }
        results.sort(Comparator.<Map<String,Object>>comparingInt(x->number(x.get("score"))).reversed().thenComparing(x->number(x.get("teamId"))));
        return results.size()>20?results.subList(0,20):results;
    }

    public List<Map<String,Object>>candidates(int teamId){
        int leader=studentId();List<Map<String,Object>>rows=jdbc.queryForList("""
            SELECT t.team_id teamId,t.leader_id leaderId,t.target_size targetSize,t.status,t.recruiting,
                   c.competition_id competitionId,c.max_team_size maxTeamSize,c.team_enabled teamEnabled,
                   c.team_open_time teamOpenTime,c.team_close_time teamCloseTime,
                   (SELECT COUNT(*) FROM team_member tm WHERE tm.team_id=t.team_id AND tm.member_status='accepted') memberCount,
                   s.major leaderMajor,s.grade leaderGrade
            FROM team t JOIN competition c ON c.competition_id=t.competition_id JOIN student s ON s.student_id=t.leader_id
            WHERE t.team_id=?
            """,teamId);
        if(rows.isEmpty())throw new TeamBusinessException("TEAM_NOT_FOUND","队伍不存在");Map<String,Object>team=rows.getFirst();
        if(number(team.get("leaderId"))!=leader)throw new TeamBusinessException("FORBIDDEN","只有负责人可以查看候选队员");
        if(!"recruiting".equals(team.get("status"))||number(team.get("recruiting"))!=1||number(team.get("teamEnabled"))!=1||number(team.get("memberCount"))>=Math.min(number(team.get("targetSize")),number(team.get("maxTeamSize"))))return List.of();
        java.time.LocalDateTime now=java.time.LocalDateTime.now();
        Object open=team.get("teamOpenTime");if(open instanceof java.sql.Timestamp t&&t.toLocalDateTime().isAfter(now))return List.of();
        Object close=team.get("teamCloseTime");if(close instanceof java.sql.Timestamp t&&!t.toLocalDateTime().isAfter(now))return List.of();
        List<Map<String,Object>>positions=jdbc.queryForList("SELECT position_id positionId,title,required_skills requiredSkills,vacancies FROM team_position WHERE team_id=? AND status='open'",teamId);
        List<Map<String,Object>>students=jdbc.queryForList("""
            SELECT s.student_id studentId,s.student_name studentName,s.major,s.grade,p.preferred_roles preferredRoles,
                   p.weekly_hours weeklyHours,COALESCE(p.show_weekly_hours,0) showWeeklyHours
            FROM student s LEFT JOIN student_team_profile p ON p.student_id=s.student_id
            WHERE s.student_id<>? AND EXISTS(SELECT 1 FROM user u WHERE u.student_id=s.student_id AND u.status='enabled' AND FIND_IN_SET('student',u.role)>0)
              AND NOT EXISTS(SELECT 1 FROM team_member tm WHERE tm.team_id=? AND tm.student_id=s.student_id AND tm.member_status='accepted')
              AND NOT EXISTS(SELECT 1 FROM team_request tr WHERE tr.team_id=? AND tr.student_id=s.student_id AND tr.status='pending')
            """,leader,teamId,teamId);
        Map<String,Integer>w=weights();List<Map<String,Object>>results=new ArrayList<>();
        for(Map<String,Object>student:students){
            int sid=number(student.get("studentId"));if(teamCount(sid,number(team.get("competitionId")))>=2)continue;
            student.put("skills",skills(sid));student.put("approvedCount",approvedCount(sid,number(team.get("competitionId"))));
            Map<String,Object>best=null;
            for(Map<String,Object>position:positions){
                if(occupied(number(position.get("positionId")))>=number(position.get("vacancies")))continue;
                Map<String,Object>match=new LinkedHashMap<>();match.put("studentId",sid);match.put("studentName",student.get("studentName"));match.put("major",student.get("major"));match.put("grade",student.get("grade"));
                match.put("positionId",position.get("positionId"));match.put("positionTitle",position.get("title"));match.put("skills",student.get("skills"));match.put("skillSource","学生自填");
                match.putAll(score(student,team,position,w,false));
                if(best==null||number(match.get("score"))>number(best.get("score")))best=match;
            }
            if(best!=null)results.add(best);
        }
        results.sort(Comparator.<Map<String,Object>>comparingInt(x->number(x.get("score"))).reversed().thenComparing(x->number(x.get("studentId"))));
        return results.size()>20?results.subList(0,20):results;
    }

    public Map<String,Object>getWeights(){admin();return new LinkedHashMap<>(jdbc.queryForMap("SELECT skill_weight skillWeight,role_weight roleWeight,time_weight timeWeight,experience_weight experienceWeight,background_weight backgroundWeight,version,update_time updateTime FROM team_recommendation_weights WHERE config_id=1"));}
    public List<Map<String,Object>>weightHistory(){admin();return jdbc.queryForList("SELECT version,skill_weight skillWeight,role_weight roleWeight,time_weight timeWeight,experience_weight experienceWeight,background_weight backgroundWeight,operator_user_id operatorUserId,create_time createTime FROM team_recommendation_weights_history ORDER BY history_id DESC LIMIT 30");}
    @Transactional
    public Map<String,Object>updateWeights(Map<String,Object>body){
        admin();int skill=value(body,"skillWeight"),role=value(body,"roleWeight"),time=value(body,"timeWeight"),experience=value(body,"experienceWeight"),background=value(body,"backgroundWeight");
        if(skill+role+time+experience+background!=100)throw new TeamBusinessException("VALIDATION_ERROR","推荐权重总和必须为100");
        int version=version(body);int changed=jdbc.update("UPDATE team_recommendation_weights SET skill_weight=?,role_weight=?,time_weight=?,experience_weight=?,background_weight=?,version=version+1,updated_by=? WHERE config_id=1 AND version=?",skill,role,time,experience,background,AuthContext.require().userId(),version);
        if(changed==0)throw new TeamBusinessException("VERSION_CONFLICT","权重已更新，请刷新后重试");
        jdbc.update("INSERT INTO team_recommendation_weights_history(version,skill_weight,role_weight,time_weight,experience_weight,background_weight,operator_user_id) VALUES(?,?,?,?,?,?,?)",version+1,skill,role,time,experience,background,AuthContext.require().userId());
        jdbc.update("INSERT INTO team_audit_log(operator_user_id,team_id,action,detail) VALUES(?,NULL,'UPDATE_RECOMMENDATION_WEIGHTS',?)",AuthContext.require().userId(),"version="+(version+1));
        return getWeights();
    }

    private Map<String,Object>score(Map<String,Object>student,Map<String,Object>team,Map<String,Object>position,Map<String,Integer>w,boolean self){
        int sid=number(student.get("studentId"));List<String>skills=self?skills(sid):(List<String>)student.get("skills");Set<String>have=new HashSet<>();for(String s:skills)have.add(s.trim().toLowerCase(Locale.ROOT));
        String raw=String.valueOf(position.getOrDefault("requiredSkills",""));List<String>required=Arrays.stream(raw.split("[,，]",-1)).map(String::trim).filter(s->!s.isEmpty()).toList();
        List<String>matched=required.stream().filter(s->have.contains(s.toLowerCase(Locale.ROOT))).toList();
        int score=required.isEmpty()?0:(int)Math.round(w.get("skill")*matched.size()/(double)required.size());List<String>reasons=new ArrayList<>();
        if(!matched.isEmpty())reasons.add("匹配技能："+String.join("、",matched));
        // 荣誉系统新增：技能等级加成（已验证技能给更高评分）
        if(!matched.isEmpty()){
            int levelBonus = skillLevelBonus(sid, matched);
            if(levelBonus>0){
                score += levelBonus;
                reasons.add("技能等级加成 +"+levelBonus);
            }
        }
        String preferred=student.get("preferredRoles")==null?"":String.valueOf(student.get("preferredRoles"));
        if(preferred.contains(String.valueOf(position.get("title")))){score+=w.get("role");reasons.add("期望岗位相符");}
        if((self||number(student.get("showWeeklyHours"))==1)&&number(student.get("weeklyHours"))>=5){score+=w.get("time");reasons.add("每周可投入时间已填写");}
        int awards=self?approvedCount(sid,number(team.get("competitionId"))):number(student.get("approvedCount"));
        if(awards>0){score+=w.get("experience");reasons.add("有系统认证的本竞赛获奖经历");}
        if(Objects.equals(student.get("major"),team.get("leaderMajor"))&&student.get("major")!=null){score+=w.get("background")/2;reasons.add("与负责人专业相同");}
        if(Objects.equals(student.get("grade"),team.get("leaderGrade"))&&student.get("grade")!=null){score+=w.get("background")-w.get("background")/2;reasons.add("与负责人年级相同");}
        boolean cold=skills.isEmpty()&&preferred.isBlank()&&awards==0;
        if(cold){
            int remaining=Math.max(0,number(team.get("targetSize"))-number(team.get("memberCount")));
            score+=Math.min(remaining,5);
            Object close=team.get("teamCloseTime");
            if(close instanceof java.sql.Timestamp deadline){
                long days=java.time.Duration.between(java.time.LocalDateTime.now(),deadline.toLocalDateTime()).toDays();
                score+=days<=7?3:days<=30?2:1;
            }
            reasons.add("岗位仍有名额");reasons.add("资料较少，推荐仅供参考");
        }
        if(reasons.isEmpty())reasons.add("该岗位仍有招募名额");
        Map<String,Object>result=new LinkedHashMap<>();result.put("score",Math.min(score,100));result.put("reasons",reasons);result.put("profileComplete",!cold);result.put("skillSource","学生自填");return result;
    }
    private Map<String,Object>student(int id){Map<String,Object>row=new LinkedHashMap<>(jdbc.queryForMap("SELECT s.student_id studentId,s.major,s.grade,p.preferred_roles preferredRoles,p.weekly_hours weeklyHours,p.show_weekly_hours showWeeklyHours FROM student s LEFT JOIN student_team_profile p ON p.student_id=s.student_id WHERE s.student_id=?",id));return row;}
    private List<String>skills(int id){return jdbc.queryForList("SELECT st.name FROM skill_tag st JOIN student_skill ss ON ss.skill_id=st.skill_id WHERE ss.student_id=?",String.class,id);}

    /**
     * 计算匹配技能的等级加成：每个已验证+advanced 的技能给 2 分，
     * verified+intermediate 给 1 分，自评无验证给 0 分
     */
    private int skillLevelBonus(int studentId, List<String> matchedSkills){
        if(matchedSkills==null||matchedSkills.isEmpty())return 0;
        List<Map<String,Object>> rows = jdbc.queryForList(
            "SELECT st.name, IFNULL(ss.verified,0) verified, IFNULL(ss.self_eval_level,'beginner') level, IFNULL(ss.experience,0) experience" +
            " FROM skill_tag st LEFT JOIN student_skill ss ON ss.skill_id=st.skill_id AND ss.student_id=?" +
            " WHERE st.name IN ("+matchedSkills.stream().map(s->"?").collect(java.util.stream.Collectors.joining(","))+")",
            java.util.stream.Stream.concat(java.util.stream.Stream.of(studentId), matchedSkills.stream()).toArray());
        int bonus=0;
        for(Map<String,Object> r:rows){
            int verified = number(r.get("verified"));
            String level = String.valueOf(r.get("level"));
            int exp = number(r.get("experience"));
            if(verified==1 && "advanced".equalsIgnoreCase(level))bonus+=3;
            else if(verified==1 && "intermediate".equalsIgnoreCase(level))bonus+=2;
            else if(verified==1)bonus+=1;
            else if(exp>=50)bonus+=1;
        }
        return bonus;
    }
    private int approvedCount(int sid,int cid){return jdbc.queryForObject("SELECT COUNT(*) FROM award_application WHERE student_id=? AND competition_id=? AND application_status='approved'",Integer.class,sid,cid);}
    private int teamCount(int sid,int cid){return jdbc.queryForObject("SELECT COUNT(*) FROM team_member tm JOIN team t ON t.team_id=tm.team_id WHERE tm.student_id=? AND t.competition_id=? AND tm.member_status='accepted' AND t.status NOT IN ('dissolved','removed')",Integer.class,sid,cid);}
    private int occupied(int pid){return jdbc.queryForObject("SELECT COUNT(*) FROM team_member WHERE position_id=? AND member_status='accepted'",Integer.class,pid);}
    private Map<String,Integer>weights(){Map<String,Object>r=jdbc.queryForMap("SELECT skill_weight,role_weight,time_weight,experience_weight,background_weight FROM team_recommendation_weights WHERE config_id=1");return Map.of("skill",number(r.get("skill_weight")),"role",number(r.get("role_weight")),"time",number(r.get("time_weight")),"experience",number(r.get("experience_weight")),"background",number(r.get("background_weight")));}
    private int studentId(){Integer id=AuthContext.require().studentId();if(id==null)throw new TeamBusinessException("STUDENT_REQUIRED","仅学生可使用推荐");return id;}
    private void admin(){if(!AuthContext.require().hasRole("admin"))throw new TeamBusinessException("FORBIDDEN","需要管理员权限");}
    private int value(Map<String,Object>body,String key){Object o=body.get(key);if(!(o instanceof Number n)||!Double.isFinite(n.doubleValue())||n.doubleValue()!=n.intValue()||n.intValue()<0||n.intValue()>100)throw new TeamBusinessException("VALIDATION_ERROR",key+"必须为0到100的整数");return n.intValue();}
    private int version(Map<String,Object>body){Object o=body.get("version");if(!(o instanceof Number n)||n.doubleValue()!=n.intValue()||n.intValue()<1)throw new TeamBusinessException("VALIDATION_ERROR","版本号无效");return n.intValue();}
    private int number(Object o){return o instanceof Boolean b?(b?1:0):o instanceof Number n?n.intValue():0;}
}
