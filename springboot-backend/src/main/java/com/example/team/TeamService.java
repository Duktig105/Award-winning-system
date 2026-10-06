package com.example.team;

import com.example.auth.AuthContext;
import com.example.auth.AuthUser;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class TeamService {
    private final JdbcTemplate jdbc;

    public TeamService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public Map<String, Object> getMyProfile() { return getProfile(currentStudentId()); }

    public Map<String, Object> getProfile(int studentId) {
        List<Map<String, Object>> rows = jdbc.queryForList("""
            SELECT s.student_id studentId, s.student_number studentNumber, s.student_name studentName,
                   s.grade, s.major, s.class_name className, s.college,
                   p.bio, p.specialties, p.competition_experience competitionExperience,
                   p.portfolio_url portfolioUrl, COALESCE(p.weekly_hours,0) weeklyHours,
                   p.preferred_roles preferredRoles, p.update_time updateTime,
                   COALESCE(p.show_bio,0) showBio,COALESCE(p.show_portfolio,0) showPortfolio,
                   COALESCE(p.show_weekly_hours,0) showWeeklyHours
            FROM student s LEFT JOIN student_team_profile p ON p.student_id=s.student_id
            WHERE s.student_id=?
            """, studentId);
        if (rows.isEmpty()) throw error("STUDENT_NOT_FOUND", "学生不存在");
        Map<String, Object> profile = new LinkedHashMap<>(rows.getFirst());
        AuthUser viewer = AuthContext.require();
        boolean owner = Objects.equals(viewer.studentId(),studentId) || viewer.hasRole("admin");
        if (!owner) {
            profile.remove("studentNumber");
            profile.remove("className");
            if (integer(profile.get("showBio"),0)==0) profile.remove("bio");
            if (integer(profile.get("showPortfolio"),0)==0) profile.remove("portfolioUrl");
            if (integer(profile.get("showWeeklyHours"),0)==0) profile.remove("weeklyHours");
            profile.remove("competitionExperience");
        }
        profile.put("skills", jdbc.queryForList("""
            SELECT st.name FROM skill_tag st JOIN student_skill ss ON ss.skill_id=st.skill_id
            WHERE ss.student_id=? ORDER BY st.name
            """, String.class, studentId));
        profile.put("approvedAwards", jdbc.queryForList("""
            SELECT c.competition_name competitionName, aa.project_name projectName,
                   aa.award_level awardLevel, aa.award_time awardTime
            FROM award_application aa JOIN competition c ON c.competition_id=aa.competition_id
            WHERE aa.student_id=? AND aa.application_status='approved' ORDER BY aa.award_time DESC LIMIT 20
            """, studentId));
        return profile;
    }

    @Transactional
    public Map<String, Object> saveProfile(Map<String, Object> body) {
        int studentId = currentStudentId();
        int weeklyHours = integer(body.get("weeklyHours"), 0);
        if (weeklyHours < 0 || weeklyHours > 168) throw error("VALIDATION_ERROR", "每周投入时间应在 0 到 168 小时之间");
        jdbc.update("""
            INSERT INTO student_team_profile(student_id,bio,specialties,competition_experience,portfolio_url,weekly_hours,preferred_roles,show_bio,show_portfolio,show_weekly_hours)
            VALUES(?,?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE bio=VALUES(bio),specialties=VALUES(specialties),
            competition_experience=VALUES(competition_experience),portfolio_url=VALUES(portfolio_url),
            weekly_hours=VALUES(weekly_hours),preferred_roles=VALUES(preferred_roles),
            show_bio=VALUES(show_bio),show_portfolio=VALUES(show_portfolio),show_weekly_hours=VALUES(show_weekly_hours)
            """, studentId, text(body,"bio",500), text(body,"specialties",500),
                text(body,"competitionExperience",1500), text(body,"portfolioUrl",500), weeklyHours,
                text(body,"preferredRoles",500), flag(body,"showBio"),flag(body,"showPortfolio"),flag(body,"showWeeklyHours"));
        jdbc.update("DELETE FROM student_skill WHERE student_id=?", studentId);
        for (String skill : stringList(body.get("skills"))) {
            String value = skill.trim();
            if (value.isEmpty()) continue;
            if (value.length() > 40) throw error("VALIDATION_ERROR", "技能标签不能超过 40 个字符");
            jdbc.update("INSERT IGNORE INTO skill_tag(name) VALUES(?)", value);
            jdbc.update("INSERT IGNORE INTO student_skill(student_id,skill_id) SELECT ?,skill_id FROM skill_tag WHERE name=?", studentId, value);
        }
        return getProfile(studentId);
    }

    public Map<String, Object> marketplace(Map<String, String> filters, int page, int pageSize) {
        page = Math.max(page, 1); pageSize = Math.min(Math.max(pageSize, 1), 50);
        StringBuilder where = new StringBuilder(" WHERE t.status IN ('recruiting','closed','locked') ");
        List<Object> args = new ArrayList<>();
        if (has(filters.get("competition"))) { where.append(" AND (c.competition_name LIKE ? OR t.name LIKE ?)"); String term="%"+filters.get("competition")+"%"; args.add(term); args.add(term); }
        addLike(where,args,"s.major",filters.get("major"));
        addLike(where,args,"s.grade",filters.get("grade"));
        if (has(filters.get("status"))) { where.append(" AND t.status=?"); args.add(filters.get("status")); }
        else where.append("""
             AND t.status='recruiting' AND t.recruiting=1
             AND (SELECT COUNT(*) FROM team_member tm2 WHERE tm2.team_id=t.team_id AND tm2.member_status='accepted') < LEAST(t.target_size,c.max_team_size)
             AND EXISTS(SELECT 1 FROM team_position tp WHERE tp.team_id=t.team_id AND tp.status='open'
                 AND (SELECT COUNT(*) FROM team_member m WHERE m.team_id=tp.team_id AND m.member_status='accepted' AND m.is_leader=0
                      AND (m.position_id=tp.position_id OR (m.position_id IS NULL AND m.role_name=tp.title))) < tp.vacancies)
            """);
        if (has(filters.get("skill"))) {
            where.append(" AND EXISTS(SELECT 1 FROM team_position tp2 WHERE tp2.team_id=t.team_id AND tp2.required_skills LIKE ?)");
            args.add("%" + filters.get("skill") + "%");
        }
        String from = """
            FROM team t JOIN competition c ON c.competition_id=t.competition_id
            LEFT JOIN student s ON s.student_id=t.leader_id
            """;
        int total = jdbc.queryForObject("SELECT COUNT(*) " + from + where, Integer.class, args.toArray());
        List<Object> listArgs = new ArrayList<>(args); listArgs.add((page-1)*pageSize); listArgs.add(pageSize);
        List<Map<String,Object>> list = jdbc.queryForList("""
            SELECT t.team_id teamId,t.name,t.description,t.status,t.recruiting,t.target_size targetSize,t.version,t.vibe_tags vibeTags,
                   c.competition_id competitionId,c.competition_name competitionName,c.min_team_size minTeamSize,
                   c.max_team_size maxTeamSize,c.team_open_time teamOpenTime,c.team_close_time teamCloseTime,c.team_enabled teamEnabled,
                   s.student_id leaderId,s.student_name leaderName,s.major,s.grade,
                   (SELECT COUNT(*) FROM team_member tm WHERE tm.team_id=t.team_id AND tm.member_status='accepted') memberCount,
                   (SELECT GROUP_CONCAT(CONCAT(tp.title,'(',(SELECT COUNT(*) FROM team_member m WHERE m.team_id=tp.team_id AND m.member_status='accepted' AND m.is_leader=0 AND (m.position_id=tp.position_id OR (m.position_id IS NULL AND m.role_name=tp.title))),'/',tp.vacancies,')') SEPARATOR '、') FROM team_position tp WHERE tp.team_id=t.team_id AND tp.status='open') positions,
                   (SELECT GROUP_CONCAT(tp.required_skills SEPARATOR ',') FROM team_position tp WHERE tp.team_id=t.team_id AND tp.status='open') requiredSkills
            """ + from + where + " ORDER BY t.update_time DESC LIMIT ?,?", listArgs.toArray());
        for (Map<String,Object> team:list) decorateAvailability(team);
        return page(list,total,page,pageSize);
    }

    public Map<String, Object> detail(int teamId) {
        Map<String,Object> team = requireTeam(teamId, false);
        team.put("members", jdbc.queryForList("""
            SELECT tm.team_member_id teamMemberId,tm.student_id studentId,tm.is_leader isLeader,
                   tm.role_name roleName,tm.position_id positionId,tm.sort_order sortOrder,tm.join_time joinTime,
                   s.student_number studentNumber,s.student_name studentName,s.grade,s.major,s.college
            FROM team_member tm JOIN student s ON s.student_id=tm.student_id
            WHERE tm.team_id=? AND tm.member_status='accepted' ORDER BY tm.is_leader DESC,tm.sort_order,tm.join_time
            """, teamId));
        List<Map<String,Object>> positions=jdbc.queryForList("SELECT position_id positionId,title,vacancies,requirements,required_skills requiredSkills,status FROM team_position WHERE team_id=? ORDER BY position_id", teamId);
        for(Map<String,Object> position:positions){
            int occupied=occupiedSeats(integer(position.get("positionId"),0),String.valueOf(position.get("title")),teamId);
            position.put("occupied",occupied);
            position.put("remainingSeats",Math.max(0,integer(position.get("vacancies"),0)-occupied));
        }
        team.put("positions",positions);
        int me = AuthContext.require().studentId() == null ? -1 : AuthContext.require().studentId();
        team.put("memberCount",((List<?>)team.get("members")).size());
        team.put("isLeader", Objects.equals(team.get("leaderId"), me));
        team.put("isMember", count("SELECT COUNT(*) FROM team_member WHERE team_id=? AND student_id=? AND member_status='accepted'", teamId, me) > 0);
        if (!Boolean.TRUE.equals(team.get("isMember")) && !AuthContext.require().hasRole("admin")) {
            for (Map<String,Object> member : (List<Map<String,Object>>)team.get("members")) member.remove("studentNumber");
        }
        decorateAvailability(team);
        return team;
    }

    public Map<String,Object> mine() {
        int studentId = currentStudentId();
        List<Map<String,Object>> teams = jdbc.queryForList("""
            SELECT t.team_id teamId,t.name,t.description,t.status,t.recruiting,t.target_size targetSize,t.vibe_tags vibeTags,
                   c.competition_name competitionName,c.team_close_time teamCloseTime,
                   CASE WHEN t.leader_id=? THEN 1 ELSE 0 END isLeader,tm.member_status memberStatus,
                   (SELECT COUNT(*) FROM team_member x WHERE x.team_id=t.team_id AND x.member_status='accepted') memberCount
            FROM team t JOIN competition c ON c.competition_id=t.competition_id
            JOIN team_member tm ON tm.team_id=t.team_id AND tm.student_id=?
            ORDER BY t.update_time DESC
            """, studentId, studentId);
        for (Map<String,Object> team:teams) team.put("historical",!"accepted".equals(team.get("memberStatus")) || List.of("dissolved","removed").contains(team.get("status")));
        return Map.of("teams", teams, "requests", requests("mine"));
    }

    @Transactional
    public int create(Map<String,Object> body) {
        int studentId = currentStudentId();
        int competitionId = requiredInt(body,"competitionId");
        Map<String,Object> competition = competition(competitionId, true);
        ensureRecruitmentOpen(competition);
        if (count("SELECT COUNT(*) FROM team WHERE competition_id=? AND leader_id=? AND status NOT IN ('dissolved','removed')", competitionId, studentId) >= 1)
            throw error("LEADER_LIMIT", "同一竞赛最多负责一支队伍");
        if (acceptedTeamCount(studentId,competitionId) >= 2) throw error("TEAM_LIMIT", "同一竞赛最多参加两支队伍");
        int min = integer(competition.get("minTeamSize"),1), max = integer(competition.get("maxTeamSize"),10);
        int target = integer(body.get("targetSize"), min);
        if (target < min || target > max) throw error("INVALID_TEAM_SIZE", "目标人数必须在竞赛人数范围内");
        String name = requiredText(body,"name",50);
        if(mapList(body.get("positions")).isEmpty()) throw error("VALIDATION_ERROR","请至少添加一个招募岗位");
        jdbc.update("INSERT INTO team(name,leader_id,competition_id,has_sort,description,status,recruiting,target_size,version,vibe_tags,create_time,update_time) VALUES(?,?,?,0,?,'recruiting',1,?,0,?,NOW(),NOW())",
                name,studentId,competitionId,text(body,"description",1000),target,text(body,"vibeTags",200));
        int teamId = jdbc.queryForObject("SELECT LAST_INSERT_ID()",Integer.class);
        jdbc.update("INSERT INTO team_member(team_id,student_id,is_leader,sort_order,role_name,member_status,join_time) VALUES(?,?,1,1,?,'accepted',NOW())",
                teamId,studentId,text(body,"leaderRole",50));
        for (Map<String,Object> position : mapList(body.get("positions"))) addPositionInternal(teamId, position);
        audit(teamId,"CREATE_TEAM",name);
        return teamId;
    }

    @Transactional
    public void update(int teamId, Map<String,Object> body) {
        Map<String,Object> team = requireLeader(teamId,true); ensureEditable(team);
        int expectedVersion = requiredInt(body,"version");
        int target = integer(body.get("targetSize"),integer(team.get("targetSize"),2));
        Map<String,Object> comp = competition(integer(team.get("competitionId"),0),false);
        if (target < integer(comp.get("minTeamSize"),1) || target > integer(comp.get("maxTeamSize"),10)) throw error("INVALID_TEAM_SIZE","目标人数不符合竞赛规则");
        if (target < count("SELECT COUNT(*) FROM team_member WHERE team_id=? AND member_status='accepted'",teamId)) throw error("INVALID_TEAM_SIZE","目标人数不能少于当前成员数");
        int changed = jdbc.update("UPDATE team SET name=?,description=?,target_size=?,vibe_tags=?,version=version+1 WHERE team_id=? AND version=?",
                requiredText(body,"name",50),text(body,"description",1000),target,text(body,"vibeTags",200),teamId,expectedVersion);
        if (changed==0) throw error("VERSION_CONFLICT","队伍信息已被其他操作更新，请刷新后重试");
        audit(teamId,"UPDATE_TEAM","更新队伍信息");
    }

    @Transactional
    public int addPosition(int teamId, Map<String,Object> body) {
        Map<String,Object> team=requireLeader(teamId,true); ensureEditable(team);
        int id=addPositionInternal(teamId,body); audit(teamId,"ADD_POSITION",String.valueOf(body.get("title"))); return id;
    }
    @Transactional
    public void updatePosition(int teamId,int positionId,Map<String,Object> body) {
        ensureEditable(requireLeader(teamId,true));
        Map<String,Object> position=position(teamId,positionId);
        int occupied=occupiedSeats(positionId,String.valueOf(position.get("title")),teamId);
        int vacancies=integer(body.get("vacancies"),integer(position.get("vacancies"),1));
        if(vacancies<1 || vacancies<occupied || vacancies>50) throw error("POSITION_FULL","岗位名额必须为1至50且不能小于已加入人数");
        String title=requiredText(body,"title",50);
        jdbc.update("UPDATE team_position SET title=?,vacancies=?,requirements=?,required_skills=? WHERE team_id=? AND position_id=?",title,vacancies,text(body,"requirements",500),text(body,"requiredSkills",500),teamId,positionId);
        jdbc.update("UPDATE team_member SET role_name=? WHERE team_id=? AND position_id=?",title,teamId,positionId);
        audit(teamId,"UPDATE_POSITION",String.valueOf(positionId));
    }
    @Transactional
    public void closePosition(int teamId,int positionId) {
        ensureEditable(requireLeader(teamId,true)); position(teamId,positionId);
        jdbc.update("UPDATE team_position SET status='closed' WHERE team_id=? AND position_id=?",teamId,positionId);
        jdbc.update("UPDATE team_request SET status='expired' WHERE position_id=? AND status='pending'",positionId);
        audit(teamId,"CLOSE_POSITION",String.valueOf(positionId));
    }
    private int addPositionInternal(int teamId,Map<String,Object> body) {
        int vacancies=integer(body.get("vacancies"),1); if(vacancies<1||vacancies>50) throw error("VALIDATION_ERROR","岗位人数无效");
        jdbc.update("INSERT INTO team_position(team_id,title,vacancies,requirements,required_skills,status) VALUES(?,?,?,?,?,'open')",
                teamId,requiredText(body,"title",50),vacancies,text(body,"requirements",500),text(body,"requiredSkills",500));
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()",Integer.class);
    }

    @Transactional
    public void deletePosition(int teamId,int positionId) {
        ensureEditable(requireLeader(teamId,true));
        if(count("SELECT COUNT(*) FROM team_request WHERE position_id=? AND status='pending'",positionId)>0 || occupiedSeats(positionId,String.valueOf(position(teamId,positionId).get("title")),teamId)>0) throw error("POSITION_IN_USE","岗位已有成员或待办请求，请关闭岗位");
        jdbc.update("DELETE FROM team_position WHERE position_id=? AND team_id=?",positionId,teamId); audit(teamId,"DELETE_POSITION",String.valueOf(positionId));
    }

    @Transactional
    public int apply(int teamId,Map<String,Object> body) {
        int studentId=currentStudentId(); Map<String,Object> team=requireTeam(teamId,true); ensureCanRecruit(team); ensureTeamHasSpace(team);
        if(Objects.equals(team.get("leaderId"),studentId)||isAcceptedMember(teamId,studentId)) throw error("ALREADY_MEMBER","你已经是该队成员");
        ensureNoPending(teamId,studentId);
        int positionId=requiredInt(body,"positionId"); requirePosition(teamId,positionId); ensurePositionHasSpace(teamId,positionId);
        String message=text(body,"message",500);
        jdbc.update("INSERT INTO team_request(team_id,student_id,position_id,request_type,status,message,operator_id) VALUES(?,?,?,'application','pending',?,?)",
                teamId,studentId,positionId,message,AuthContext.require().userId());
        int requestId=jdbc.queryForObject("SELECT LAST_INSERT_ID()",Integer.class);
        // 通知正文带上申请人姓名与留言摘要，链接直达申请与邀请列表并定位该条请求，避免队长点开消息后找不到留言
        String applicant=jdbc.queryForObject("SELECT student_name FROM student WHERE student_id=?",String.class,studentId);
        String preview=message==null?"":message.length()>80?message.substring(0,80)+"…":message;
        notifyStudent(integer(team.get("leaderId"),0),"TEAM_APPLICATION","收到新的入队申请",
                "“"+applicant+"”申请加入“"+team.get("name")+"”"+(preview.isEmpty()?"":"，留言："+preview),
                "/student/teams?tab=requests&requestId="+requestId);
        audit(teamId,"APPLY_TEAM","requestId="+requestId); return requestId;
    }

    @Transactional
    public int invite(int teamId,Map<String,Object> body) {
        Map<String,Object> team=requireLeader(teamId,true); ensureCanRecruit(team); ensureTeamHasSpace(team);
        Integer targetId=nullableInt(body.get("targetStudentId"));
        String number=targetId==null?requiredText(body,"studentNumber",30):null;
        List<Map<String,Object>> students=targetId==null
                ? jdbc.queryForList("SELECT s.student_id studentId,s.student_name studentName FROM student s WHERE s.student_number=? AND EXISTS(SELECT 1 FROM user u WHERE u.student_id=s.student_id AND u.status='enabled' AND FIND_IN_SET('student',u.role)>0)",number)
                : jdbc.queryForList("SELECT s.student_id studentId,s.student_name studentName FROM student s WHERE s.student_id=? AND EXISTS(SELECT 1 FROM user u WHERE u.student_id=s.student_id AND u.status='enabled' AND FIND_IN_SET('student',u.role)>0)",targetId);
        if(students.isEmpty()) throw error("STUDENT_NOT_FOUND","未找到拥有系统账号的学生");
        int studentId=integer(students.getFirst().get("studentId"),0);
        if(isAcceptedMember(teamId,studentId)) throw error("ALREADY_MEMBER","该学生已经是队伍成员"); ensureNoPending(teamId,studentId);
        Integer positionId=nullableInt(body.get("positionId")); if(positionId!=null) { requirePosition(teamId,positionId); ensurePositionHasSpace(teamId,positionId); }
        String message=text(body,"message",500);
        jdbc.update("INSERT INTO team_request(team_id,student_id,position_id,request_type,status,message,operator_id) VALUES(?,?,?,'invitation','pending',?,?)",
                teamId,studentId,positionId,message,AuthContext.require().userId());
        int id=jdbc.queryForObject("SELECT LAST_INSERT_ID()",Integer.class);
        // 邀请说明同样以摘要形式进入通知正文，链接直达申请与邀请列表定位该条邀请
        String preview=message==null?"":message.length()>80?message.substring(0,80)+"…":message;
        notifyStudent(studentId,"TEAM_INVITATION","收到组队邀请",
                "“"+team.get("name")+"”邀请你加入队伍"+(preview.isEmpty()?"":"，留言："+preview),
                "/student/teams?tab=requests&requestId="+id);
        audit(teamId,"INVITE_STUDENT","studentId="+studentId); return id;
    }

    public List<Map<String,Object>> requests(String scope) {
        jdbc.update("UPDATE team_request tr JOIN team t ON t.team_id=tr.team_id JOIN competition c ON c.competition_id=t.competition_id SET tr.status='expired' WHERE tr.status='pending' AND (t.status<>'recruiting' OR t.recruiting=0 OR c.team_enabled=0 OR (c.team_close_time IS NOT NULL AND c.team_close_time<NOW()))");
        // 岗位维度收口：岗位已关闭或名额已满时，该岗位的待处理请求自动失效，避免其他人继续竞争已确认的岗位
        jdbc.update("""
            UPDATE team_request tr JOIN team_position tp ON tp.position_id=tr.position_id SET tr.status='expired'
            WHERE tr.status='pending' AND (tp.status<>'open'
                OR (SELECT COUNT(*) FROM team_member m WHERE m.team_id=tr.team_id AND m.member_status='accepted' AND m.is_leader=0
                    AND (m.position_id=tp.position_id OR (m.position_id IS NULL AND m.role_name=tp.title))) >= tp.vacancies)
            """);
        AuthUser user=AuthContext.require(); int studentId=currentStudentId();
        String where=" WHERE tr.student_id=? "; List<Object> args=new ArrayList<>(List.of(studentId));
        if("managed".equals(scope)){where=" WHERE t.leader_id=? ";args=new ArrayList<>(List.of(studentId));}
        List<Map<String,Object>> result=jdbc.queryForList("""
            SELECT tr.request_id requestId,tr.request_type requestType,tr.status,tr.message,tr.create_time createTime,tr.operator_id operatorId,
                   tr.student_id studentId,s.student_number studentNumber,s.student_name studentName,s.grade,s.major,
                    t.team_id teamId,t.name teamName,t.leader_id leaderId,c.competition_name competitionName,
                   tp.title positionTitle,tp.vacancies positionVacancies,tp.status positionStatus,
                   (SELECT COUNT(*) FROM team_member m WHERE m.team_id=tr.team_id AND m.member_status='accepted' AND m.is_leader=0
                        AND (m.position_id=tr.position_id OR (m.position_id IS NULL AND m.role_name=tp.title))) positionOccupied
            FROM team_request tr JOIN team t ON t.team_id=tr.team_id JOIN competition c ON c.competition_id=t.competition_id
            JOIN student s ON s.student_id=tr.student_id LEFT JOIN team_position tp ON tp.position_id=tr.position_id
            """+where+" ORDER BY CASE tr.status WHEN 'pending' THEN 0 ELSE 1 END,tr.create_time DESC",args.toArray());
        if("managed".equals(scope)) for(Map<String,Object> request:result) request.remove("studentNumber");
        return result;
    }

    @Transactional
    public void decideRequest(int requestId,String action) {
        List<Map<String,Object>> identity=jdbc.queryForList("SELECT student_id studentId,team_id teamId FROM team_request WHERE request_id=?",requestId);
        if(identity.isEmpty()) throw error("REQUEST_NOT_FOUND","请求不存在");
        int sid=integer(identity.getFirst().get("studentId"),0);
        jdbc.queryForObject("SELECT student_id FROM student WHERE student_id=? FOR UPDATE",Integer.class,sid);
        requireTeam(integer(identity.getFirst().get("teamId"),0),true);
        Map<String,Object> req=lockRequest(requestId); String status=String.valueOf(req.get("status"));
        if(!"pending".equals(status)) throw error("REQUEST_PROCESSED","该请求已处理");
        int teamId=integer(req.get("teamId"),0), studentId=integer(req.get("studentId"),0);
        String type=String.valueOf(req.get("requestType")); AuthUser me=AuthContext.require();
        boolean mayDecide="cancel".equals(action) ? Objects.equals(req.get("operatorId"),me.userId())
                : "application".equals(type)?Objects.equals(req.get("leaderId"),me.studentId()):Objects.equals(studentId,me.studentId());
        if(!mayDecide) throw error("FORBIDDEN","无权处理该请求");
        if("accept".equals(action)) acceptMember(req);
        else if("reject".equals(action)) { jdbc.update("UPDATE team_request SET status='rejected' WHERE request_id=?",requestId); notifyRequestResult(req,"申请或邀请已被拒绝"); }
        else if("cancel".equals(action)) { jdbc.update("UPDATE team_request SET status='cancelled' WHERE request_id=?",requestId); notifyStudent(studentId,"TEAM_REQUEST_CANCELLED","组队请求已撤回","申请或邀请已被发起人撤回","/student/teams?tab=requests"); }
        else throw error("VALIDATION_ERROR","不支持的处理动作");
        audit(teamId,"REQUEST_"+action.toUpperCase(),"requestId="+requestId);
    }

    private void acceptMember(Map<String,Object> req) {
        int teamId=integer(req.get("teamId"),0),studentId=integer(req.get("studentId"),0),competitionId=integer(req.get("competitionId"),0);
        Map<String,Object> team=requireTeam(teamId,true); ensureCanRecruit(team);
        if(isAcceptedMember(teamId,studentId)) throw error("ALREADY_MEMBER","该学生已经是队伍成员");
        ensureTeamHasSpace(team);
        if(acceptedTeamCount(studentId,competitionId)>=2) throw error("TEAM_LIMIT","该学生在本竞赛已参加两支队伍");
        Integer positionId=nullableInt(req.get("positionId"));
        if(positionId!=null){requirePosition(teamId,positionId);ensurePositionHasSpace(teamId,positionId);}
        String role=positionId==null?"队员":String.valueOf(position(teamId,positionId).get("title"));
        if(count("SELECT COUNT(*) FROM team_member WHERE team_id=? AND student_id=?",teamId,studentId)>0)
            jdbc.update("UPDATE team_member SET role_name=?,position_id=?,member_status='accepted',join_time=NOW() WHERE team_id=? AND student_id=?",role,positionId,teamId,studentId);
        else jdbc.update("INSERT INTO team_member(team_id,student_id,is_leader,sort_order,role_name,position_id,member_status,join_time) VALUES(?,?,0,NULL,?,?,'accepted',NOW())",teamId,studentId,role,positionId);
        jdbc.update("UPDATE team_request SET status='accepted' WHERE request_id=?",req.get("requestId"));
        int filled=count("SELECT COUNT(*) FROM team_member WHERE team_id=? AND member_status='accepted'",teamId);
        int limit=Math.min(integer(team.get("targetSize"),2),integer(team.get("maxTeamSize"),10));
        if(filled>=limit||!hasOpenSeat(teamId)){
            // 满员（目标人数已满，或所有开放岗位均招满）：自动从组队广场下架。
            // 持久化 recruiting=0 + status='closed'，此后即使有成员退出也不会自动重新上架，
            // 队长如需继续扩充，需在“我的队伍”中手动重开招募。
            jdbc.update("UPDATE team SET recruiting=0,status='closed',version=version+1 WHERE team_id=?",teamId);
            jdbc.update("UPDATE team_request SET status='expired' WHERE team_id=? AND status='pending'",teamId);
            notifyTeam(teamId,"TEAM_DELISTED","队伍已满员下架","“"+team.get("name")+"”岗位已全部落实，已从组队广场下架；如需继续扩充，可在“我的队伍”重新开启招募");
            audit(teamId,"TEAM_FULL_DELIST","requestId="+req.get("requestId"));
        }
        else if(positionId!=null) {Map<String,Object>position=position(teamId,positionId);if(occupiedSeats(positionId,String.valueOf(position.get("title")),teamId)>=integer(position.get("vacancies"),1)) jdbc.update("UPDATE team_request SET status='expired' WHERE position_id=? AND status='pending'",positionId);}
        if(acceptedTeamCount(studentId,competitionId)>=2) jdbc.update("""
            UPDATE team_request tr JOIN team t ON t.team_id=tr.team_id SET tr.status='expired'
            WHERE tr.student_id=? AND t.competition_id=? AND tr.status='pending' AND tr.request_id<>?
            """,studentId,competitionId,req.get("requestId"));
        notifyStudent(studentId,"TEAM_JOINED","已加入队伍","你已加入“"+team.get("name")+"”","/student/teams?teamId="+teamId);
        notifyStudent(integer(team.get("leaderId"),0),"TEAM_MEMBER_CHANGE","新成员加入","队伍“"+team.get("name")+"”有新成员加入","/student/teams?teamId="+teamId);
    }

    @Transactional
    public void removeMember(int teamId,int studentId) {
        Map<String,Object> team=requireLeader(teamId,true); ensureEditable(team);
        if(Objects.equals(team.get("leaderId"),studentId)) throw error("LEADER_CANNOT_LEAVE","负责人必须先转让负责人身份");
        int changed=jdbc.update("UPDATE team_member SET member_status='removed' WHERE team_id=? AND student_id=? AND member_status='accepted'",teamId,studentId);
        if(changed==0) throw error("MEMBER_NOT_FOUND","成员不存在");
        notifyStudent(studentId,"TEAM_MEMBER_CHANGE","已离开队伍","你已被移出“"+team.get("name")+"”","/student/teams"); audit(teamId,"REMOVE_MEMBER",String.valueOf(studentId));
    }
    @Transactional
    public void leave(int teamId) {
        int sid=currentStudentId(); Map<String,Object> team=requireTeam(teamId,true);ensureEditable(team);
        if(!isAcceptedMember(teamId,sid)) throw error("MEMBER_NOT_FOUND","你不是该队成员");
        if(Objects.equals(team.get("leaderId"),sid)) {
            if(count("SELECT COUNT(*) FROM team_member WHERE team_id=? AND member_status='accepted'",teamId)>1) throw error("LEADER_CANNOT_LEAVE","请先将负责人转让给正式成员");
            jdbc.update("UPDATE team SET status='dissolved',recruiting=0,version=version+1 WHERE team_id=?",teamId);
            jdbc.update("UPDATE team_request SET status='expired' WHERE team_id=? AND status='pending'",teamId);
        } else {
            jdbc.update("UPDATE team_member SET member_status='left' WHERE team_id=? AND student_id=?",teamId,sid);
            notifyStudent(integer(team.get("leaderId"),0),"TEAM_MEMBER_CHANGE","成员退出","有成员退出“"+team.get("name")+"”","/student/teams?teamId="+teamId);
        }
        audit(teamId,"LEAVE_TEAM",String.valueOf(sid));
    }
    @Transactional
    public void changeMemberRole(int teamId,int studentId,Map<String,Object> body) {
        Map<String,Object> team=requireLeader(teamId,true);ensureEditable(team);
        if(!isAcceptedMember(teamId,studentId) || Objects.equals(team.get("leaderId"),studentId)) throw error("MEMBER_NOT_FOUND","只能调整普通正式成员");
        Integer positionId=nullableInt(body.get("positionId"));
        String role=requiredText(body,"roleName",50);
        if(positionId!=null) {
            requirePosition(teamId,positionId);
            Integer current=jdbc.queryForObject("SELECT position_id FROM team_member WHERE team_id=? AND student_id=? AND member_status='accepted'",Integer.class,teamId,studentId);
            if(!Objects.equals(current,positionId)) ensurePositionHasSpace(teamId,positionId);
            role=String.valueOf(position(teamId,positionId).get("title"));
        }
        jdbc.update("UPDATE team_member SET position_id=?,role_name=? WHERE team_id=? AND student_id=?",positionId,role,teamId,studentId);
        notifyStudent(studentId,"TEAM_ROLE_CHANGE","岗位已调整","你在“"+team.get("name")+"”的岗位已调整为"+role,"/student/teams?teamId="+teamId);
        audit(teamId,"CHANGE_MEMBER_ROLE",studentId+":"+role);
    }

    @Transactional
    public void transfer(int teamId,int studentId) {
        Map<String,Object> team=requireLeader(teamId,true); ensureEditable(team);
        if(!isAcceptedMember(teamId,studentId)) throw error("MEMBER_NOT_FOUND","只能转让给正式成员");
        int competitionId=integer(team.get("competitionId"),0);
        if(count("SELECT COUNT(*) FROM team WHERE competition_id=? AND leader_id=? AND status NOT IN ('dissolved','removed')",competitionId,studentId)>0) throw error("LEADER_LIMIT","该成员已负责本竞赛另一支队伍");
        int old=currentStudentId(); jdbc.update("UPDATE team SET leader_id=?,version=version+1 WHERE team_id=?",studentId,teamId);
        jdbc.update("UPDATE team_member SET is_leader=CASE WHEN student_id=? THEN 1 ELSE 0 END WHERE team_id=?",studentId,teamId);
        notifyStudent(studentId,"LEADER_TRANSFER","你已成为负责人","你已成为“"+team.get("name")+"”负责人","/student/teams?teamId="+teamId); audit(teamId,"TRANSFER_LEADER",old+"->"+studentId);
    }

    @Transactional
    public void setRecruiting(int teamId,boolean recruiting) {
        Map<String,Object> team=requireLeader(teamId,true); ensureEditable(team);
        if(recruiting) ensureRecruitmentOpen(competition(integer(team.get("competitionId"),0),false));
        jdbc.update("UPDATE team SET recruiting=?,status=?,version=version+1 WHERE team_id=?",recruiting?1:0,recruiting?"recruiting":"closed",teamId); audit(teamId,recruiting?"OPEN_RECRUITING":"CLOSE_RECRUITING","");
        if(!recruiting) jdbc.update("UPDATE team_request SET status='expired' WHERE team_id=? AND status='pending'",teamId);
    }

    @Transactional
    public void lock(int teamId) {
        Map<String,Object> team=requireLeader(teamId,true); ensureEditable(team);
        int members=count("SELECT COUNT(*) FROM team_member WHERE team_id=? AND member_status='accepted'",teamId);
        if(members<integer(team.get("minTeamSize"),1)) throw error("TEAM_TOO_SMALL","队伍尚未达到竞赛最低人数");
        if(count("SELECT COUNT(*) FROM team_request WHERE team_id=? AND status='pending'",teamId)>0) throw error("PENDING_REQUESTS","请先处理所有待办申请和邀请");
        jdbc.update("UPDATE team SET status='locked',recruiting=0,locked_time=NOW(),version=version+1 WHERE team_id=?",teamId);
        notifyTeam(teamId,"TEAM_LOCKED","队伍已锁定","“"+team.get("name")+"”已锁定，可用于获奖申报"); audit(teamId,"LOCK_TEAM","");
    }

    @Transactional
    public void dissolve(int teamId) {
        Map<String,Object> team=requireLeader(teamId,true); ensureEditable(team);
        jdbc.update("UPDATE team SET status='dissolved',recruiting=0,version=version+1 WHERE team_id=?",teamId);
        jdbc.update("UPDATE team_request SET status='expired' WHERE team_id=? AND status='pending'",teamId);
        notifyTeam(teamId,"TEAM_DISSOLVED","队伍已解散","“"+team.get("name")+"”已解散"); audit(teamId,"DISSOLVE_TEAM","");
    }

    @Transactional
    public void report(int teamId,String reason) {
        requireTeam(teamId,false); String value=reason==null?"":reason.trim(); if(value.isEmpty()||value.length()>500) throw error("VALIDATION_ERROR","请填写 1-500 字举报原因");
        jdbc.update("INSERT INTO team_report(team_id,reporter_user_id,reason,status) VALUES(?,?,?,'pending')",teamId,AuthContext.require().userId(),value); audit(teamId,"REPORT_TEAM",value);
    }

    public List<Map<String,Object>> lockedTeams() {
        int sid=currentStudentId(); return jdbc.queryForList("""
            SELECT DISTINCT t.team_id teamId,t.name teamName,c.competition_id competitionId,c.competition_name competitionName,c.award_rank awardRank,t.locked_time lockedTime
            FROM team t JOIN competition c ON c.competition_id=t.competition_id JOIN team_member tm ON tm.team_id=t.team_id
            WHERE t.status='locked' AND tm.student_id=? AND tm.member_status='accepted' ORDER BY t.locked_time DESC
            """,sid);
    }

    public Map<String,Object> lockedTeamForApplication(int teamId) {
        Map<String,Object> team=requireTeam(teamId,true);
        if(!"locked".equals(String.valueOf(team.get("status")))) throw error("TEAM_NOT_LOCKED","只能选择已锁定队伍");
        if(!isAcceptedMember(teamId,currentStudentId())) throw error("FORBIDDEN","你不是该队正式成员");
        team.put("members",jdbc.queryForList("SELECT tm.student_id studentId,tm.is_leader isLeader,tm.sort_order sortOrder,s.student_number studentNumber,s.student_name studentName,s.college FROM team_member tm JOIN student s ON s.student_id=tm.student_id WHERE tm.team_id=? AND tm.member_status='accepted' ORDER BY tm.is_leader DESC,tm.sort_order,tm.join_time",teamId));
        return team;
    }

    // Admin operations
    public Map<String,Object> adminTeams(String keyword,String status,int page,int pageSize) {
        requireAdmin(); page=Math.max(page,1);pageSize=Math.min(Math.max(pageSize,1),50);String where=" WHERE 1=1 ";List<Object>a=new ArrayList<>();
        if(has(keyword)){where+=" AND (t.name LIKE ? OR c.competition_name LIKE ? OR s.student_number LIKE ?)";for(int i=0;i<3;i++)a.add("%"+keyword+"%");}
        if(has(status)){where+=" AND t.status=?";a.add(status);}int total=jdbc.queryForObject("SELECT COUNT(*) FROM team t JOIN competition c ON c.competition_id=t.competition_id LEFT JOIN student s ON s.student_id=t.leader_id"+where,Integer.class,a.toArray());
        List<Object>b=new ArrayList<>(a);b.add((page-1)*pageSize);b.add(pageSize);
        List<Map<String,Object>>list=jdbc.queryForList("SELECT t.team_id teamId,t.name,t.status,t.recruiting,t.target_size targetSize,t.update_time updateTime,c.competition_name competitionName,s.student_name leaderName,s.student_number leaderNumber,(SELECT COUNT(*) FROM team_member tm WHERE tm.team_id=t.team_id) memberCount FROM team t JOIN competition c ON c.competition_id=t.competition_id LEFT JOIN student s ON s.student_id=t.leader_id"+where+" ORDER BY t.update_time DESC LIMIT ?,?",b.toArray());
        return page(list,total,page,pageSize);
    }

    @Transactional
    public void adminAction(int teamId,String action,String reason) {
        requireAdmin();Map<String,Object>team=requireTeam(teamId,true);
        String currentStatus=String.valueOf(team.get("status"));
        if("restore".equals(action)&&!"removed".equals(currentStatus)) throw error("INVALID_TEAM_STATE","只有已下架队伍可以恢复");
        if("unlock".equals(action)&&!"locked".equals(currentStatus)) throw error("INVALID_TEAM_STATE","只有已锁定队伍可以解锁");
        if("remove".equals(action)&&List.of("removed","dissolved").contains(currentStatus)) throw error("INVALID_TEAM_STATE","当前队伍无法下架");
        if("dissolve".equals(action)&&"dissolved".equals(currentStatus)) throw error("INVALID_TEAM_STATE","队伍已经解散");
        switch(action){
            case "remove"->jdbc.update("UPDATE team SET status='removed',recruiting=0,removed_reason=?,version=version+1 WHERE team_id=?",reason,teamId);
            case "restore"->jdbc.update("UPDATE team SET status='closed',recruiting=0,removed_reason=NULL,version=version+1 WHERE team_id=?",teamId);
            case "unlock"->jdbc.update("UPDATE team SET status='closed',locked_time=NULL,version=version+1 WHERE team_id=?",teamId);
            case "dissolve"->jdbc.update("UPDATE team SET status='dissolved',recruiting=0,version=version+1 WHERE team_id=?",teamId);
            default->throw error("VALIDATION_ERROR","不支持的管理操作");
        }
        if(List.of("remove","dissolve").contains(action)) jdbc.update("UPDATE team_request SET status='expired' WHERE team_id=? AND status='pending'",teamId);
        notifyTeam(teamId,"ADMIN_TEAM_ACTION","队伍状态发生变化","管理员已对“"+team.get("name")+"”执行："+action+(has(reason)?"，原因："+reason:""));audit(teamId,"ADMIN_"+action.toUpperCase(),reason);
    }

    public List<Map<String,Object>> adminReports(){requireAdmin();return jdbc.queryForList("SELECT r.report_id reportId,r.reason,r.status,r.resolution,r.create_time createTime,t.team_id teamId,t.name teamName,u.username reporter FROM team_report r JOIN team t ON t.team_id=r.team_id JOIN user u ON u.user_id=r.reporter_user_id ORDER BY CASE r.status WHEN 'pending' THEN 0 ELSE 1 END,r.create_time DESC");}
    @Transactional public void resolveReport(int id,String status,String resolution){requireAdmin();if(!List.of("resolved","rejected").contains(status))throw error("VALIDATION_ERROR","处理状态无效");List<Map<String,Object>>report=jdbc.queryForList("SELECT team_id teamId,status FROM team_report WHERE report_id=? FOR UPDATE",id);if(report.isEmpty()||!"pending".equals(report.getFirst().get("status")))throw error("REPORT_PROCESSED","举报不存在或已处理");jdbc.update("UPDATE team_report SET status=?,resolution=?,resolved_time=NOW() WHERE report_id=?",status,resolution,id);audit(integer(report.getFirst().get("teamId"),0),"RESOLVE_REPORT",id+":"+status+":"+resolution);}
    public List<Map<String,Object>> auditLogs(int teamId){requireAdmin();return jdbc.queryForList("SELECT a.audit_id auditId,a.action,a.detail,a.create_time createTime,u.username operator FROM team_audit_log a JOIN user u ON u.user_id=a.operator_user_id WHERE a.team_id=? ORDER BY a.create_time DESC",teamId);}

    // Internal helpers
    private Map<String,Object> requireTeam(int id,boolean lock){List<Map<String,Object>>r=jdbc.queryForList("""
        SELECT t.team_id teamId,t.name,t.description,t.leader_id leaderId,t.competition_id competitionId,t.status,t.recruiting,t.target_size targetSize,t.version,t.vibe_tags vibeTags,
               c.competition_name competitionName,c.min_team_size minTeamSize,c.max_team_size maxTeamSize,c.team_open_time teamOpenTime,c.team_close_time teamCloseTime,c.team_enabled teamEnabled
        FROM team t JOIN competition c ON c.competition_id=t.competition_id WHERE t.team_id=?
        """+(lock?" FOR UPDATE":""),id);if(r.isEmpty())throw error("TEAM_NOT_FOUND","队伍不存在");return new LinkedHashMap<>(r.getFirst());}
    private Map<String,Object> requireLeader(int id,boolean lock){Map<String,Object>t=requireTeam(id,lock);if(!Objects.equals(t.get("leaderId"),currentStudentId()))throw error("FORBIDDEN","只有负责人可以执行此操作");return t;}
    private Map<String,Object> competition(int id,boolean lock){List<Map<String,Object>>r=jdbc.queryForList("SELECT competition_id competitionId,competition_name competitionName,min_team_size minTeamSize,max_team_size maxTeamSize,team_open_time teamOpenTime,team_close_time teamCloseTime,team_enabled teamEnabled FROM competition WHERE competition_id=?"+(lock?" FOR UPDATE":""),id);if(r.isEmpty())throw error("COMPETITION_NOT_FOUND","竞赛不存在");return r.getFirst();}
    private void ensureRecruitmentOpen(Map<String,Object>c){if(integer(c.get("teamEnabled"),0)!=1)throw error("RECRUITMENT_DISABLED","该竞赛未开放组队");LocalDateTime now=LocalDateTime.now();LocalDateTime open=date(c.get("teamOpenTime")),close=date(c.get("teamCloseTime"));if(open!=null&&now.isBefore(open))throw error("RECRUITMENT_NOT_STARTED","尚未到组队开放时间");if(close!=null&&now.isAfter(close))throw error("RECRUITMENT_CLOSED","组队时间已截止");}
    private void ensureEditable(Map<String,Object>t){if(!List.of("recruiting","closed").contains(String.valueOf(t.get("status"))))throw error("TEAM_LOCKED","当前队伍状态不允许修改");}
    private void ensureCanRecruit(Map<String,Object>t){ensureEditable(t);if(integer(t.get("recruiting"),0)!=1)throw error("RECRUITMENT_CLOSED","队伍已关闭招募");ensureRecruitmentOpen(t);}
    private void ensureNoPending(int teamId,int sid){if(count("SELECT COUNT(*) FROM team_request WHERE team_id=? AND student_id=? AND status='pending'",teamId,sid)>0)throw error("DUPLICATE_REQUEST","已有待处理申请或邀请");}
    private void requirePosition(int teamId,int positionId){if(count("SELECT COUNT(*) FROM team_position WHERE position_id=? AND team_id=? AND status='open'",positionId,teamId)==0)throw error("POSITION_NOT_FOUND","岗位不存在或已关闭");}
    private Map<String,Object>lockRequest(int id){List<Map<String,Object>>r=jdbc.queryForList("""
        SELECT tr.request_id requestId,tr.team_id teamId,tr.student_id studentId,tr.position_id positionId,tr.request_type requestType,tr.status,tr.operator_id operatorId,
               t.leader_id leaderId,t.competition_id competitionId,tp.title positionTitle FROM team_request tr JOIN team t ON t.team_id=tr.team_id LEFT JOIN team_position tp ON tp.position_id=tr.position_id WHERE tr.request_id=? FOR UPDATE
        """,id);if(r.isEmpty())throw error("REQUEST_NOT_FOUND","请求不存在");return r.getFirst();}
    private boolean isAcceptedMember(int tid,int sid){return count("SELECT COUNT(*) FROM team_member WHERE team_id=? AND student_id=? AND member_status='accepted'",tid,sid)>0;}
    private Map<String,Object>position(int teamId,int positionId){List<Map<String,Object>>rows=jdbc.queryForList("SELECT position_id positionId,title,vacancies,status FROM team_position WHERE team_id=? AND position_id=?",teamId,positionId);if(rows.isEmpty())throw error("POSITION_NOT_FOUND","岗位不存在");return rows.getFirst();}
    private int occupiedSeats(int positionId,String title,int teamId){return count("SELECT COUNT(*) FROM team_member WHERE team_id=? AND member_status='accepted' AND is_leader=0 AND (position_id=? OR (position_id IS NULL AND role_name=?))",teamId,positionId,title);}
    private void ensurePositionHasSpace(int teamId,int positionId){Map<String,Object>p=position(teamId,positionId);if(!"open".equals(p.get("status")))throw error("POSITION_CLOSED","岗位已关闭");if(occupiedSeats(positionId,String.valueOf(p.get("title")),teamId)>=integer(p.get("vacancies"),1))throw error("POSITION_FULL","该岗位已满");}
    private void ensureTeamHasSpace(Map<String,Object>team){int teamId=integer(team.get("teamId"),0);int limit=Math.min(integer(team.get("targetSize"),2),integer(team.get("maxTeamSize"),10));if(count("SELECT COUNT(*) FROM team_member WHERE team_id=? AND member_status='accepted'",teamId)>=limit)throw error("TEAM_FULL","队伍目标人数已满");}
    private void decorateAvailability(Map<String,Object>team){
        String reason=null;String status=String.valueOf(team.get("status"));
        if(!"recruiting".equals(status)||integer(team.get("recruiting"),0)!=1)reason="队伍未开放招募";
        else if(integer(team.get("memberCount"),0)>=Math.min(integer(team.get("targetSize"),2),integer(team.get("maxTeamSize"),10)))reason="队伍人数已满";
        else if(integer(team.get("teamEnabled"),1)!=1)reason="竞赛未开放组队";
        else {LocalDateTime now=LocalDateTime.now(),open=date(team.get("teamOpenTime")),close=date(team.get("teamCloseTime"));if(open!=null&&now.isBefore(open))reason="招募尚未开始";else if(close!=null&&now.isAfter(close))reason="招募已截止";}
        int teamId=integer(team.get("teamId"),0);
        if(reason==null && !hasOpenSeat(teamId)) reason="暂无开放岗位";
        team.put("canApply",reason==null);team.put("unavailableReason",reason);
        // 招募状态（派生值，查询时实时计算）：fresh=尚无成员落位；partial=队长意向职位部分已落实、仍有空缺；full=已无任何名额
        boolean positioned=count("SELECT COUNT(*) FROM team_member WHERE team_id=? AND member_status='accepted' AND is_leader=0 AND position_id IS NOT NULL",teamId)>0;
        if(!hasOpenSeat(teamId)||integer(team.get("memberCount"),0)>=Math.min(integer(team.get("targetSize"),2),integer(team.get("maxTeamSize"),10))) team.put("recruitState","full");
        else team.put("recruitState",positioned?"partial":"fresh");
    }
    /** 是否仍有可落座的开放岗位（岗位未关且占位数小于名额） */
    private boolean hasOpenSeat(int teamId){return count("SELECT COUNT(*) FROM team_position p WHERE p.team_id=? AND p.status='open' AND (SELECT COUNT(*) FROM team_member m WHERE m.team_id=p.team_id AND m.member_status='accepted' AND m.is_leader=0 AND (m.position_id=p.position_id OR (m.position_id IS NULL AND m.role_name=p.title)))<p.vacancies",teamId)>0;}
    private int acceptedTeamCount(int sid,int cid){return count("SELECT COUNT(DISTINCT tm.team_id) FROM team_member tm JOIN team t ON t.team_id=tm.team_id WHERE tm.student_id=? AND t.competition_id=? AND tm.member_status='accepted' AND t.status NOT IN ('dissolved','removed')",sid,cid);}
    private int currentStudentId(){Integer id=AuthContext.require().studentId();if(id==null)throw error("STUDENT_REQUIRED","该功能仅面向学生");return id;}
    private void requireAdmin(){if(!AuthContext.require().hasRole("admin"))throw error("FORBIDDEN","需要管理员权限");}
    private void audit(Integer teamId,String action,String detail){jdbc.update("INSERT INTO team_audit_log(operator_user_id,team_id,action,detail) VALUES(?,?,?,?)",AuthContext.require().userId(),teamId,action,detail);}
    private void notifyStudent(int sid,String type,String title,String content,String link){jdbc.update("INSERT INTO system_notification(user_id,type,title,content,link) SELECT user_id,?,?,?,? FROM user WHERE student_id=?",type,title,content,link,sid);}
    private void notifyTeam(int teamId,String type,String title,String content){jdbc.update("INSERT INTO system_notification(user_id,type,title,content,link) SELECT DISTINCT u.user_id,?,?,?,CONCAT('/student/teams?teamId=',?) FROM team_member tm JOIN user u ON u.student_id=tm.student_id WHERE tm.team_id=?",type,title,content,teamId,teamId);}
    private void notifyRequestResult(Map<String,Object>r,String content){notifyStudent(integer(r.get("studentId"),0),"TEAM_REQUEST_RESULT","组队请求状态更新",content,"/student/teams");}
    private int count(String sql,Object...args){return Optional.ofNullable(jdbc.queryForObject(sql,Integer.class,args)).orElse(0);}
    private Map<String,Object>page(List<Map<String,Object>>list,int total,int page,int size){return Map.of("list",list,"total",total,"page",page,"pageSize",size);}
    private void addLike(StringBuilder w,List<Object>a,String col,String value){if(has(value)){w.append(" AND ").append(col).append(" LIKE ?");a.add("%"+value+"%");}}
    private boolean has(String s){return s!=null&&!s.trim().isEmpty();}
    private TeamBusinessException error(String code,String msg){return new TeamBusinessException(code,msg);}
    private int requiredInt(Map<String,Object>b,String k){Integer v=nullableInt(b.get(k));if(v==null)throw error("VALIDATION_ERROR",k+"不能为空");return v;}
    private Integer nullableInt(Object v){if(v==null||String.valueOf(v).isBlank())return null;if(v instanceof Boolean b)return b?1:0;return v instanceof Number n?n.intValue():Integer.valueOf(String.valueOf(v));}
    private int integer(Object v,int d){Integer x=nullableInt(v);return x==null?d:x;}
    private int flag(Map<String,Object>body,String key){Object value=body.get(key);return Boolean.TRUE.equals(value)||Objects.equals(value,1)?1:0;}
    private String requiredText(Map<String,Object>b,String k,int max){String v=text(b,k,max);if(v==null||v.isBlank())throw error("VALIDATION_ERROR",k+"不能为空");return v;}
    private String text(Map<String,Object>b,String k,int max){Object x=b.get(k);if(x==null)return null;String v=String.valueOf(x).trim();if(v.length()>max)throw error("VALIDATION_ERROR",k+"长度不能超过"+max);return v.isEmpty()?null:v;}
    private List<String>stringList(Object v){if(v instanceof List<?>l)return l.stream().map(String::valueOf).toList();return List.of();}
    @SuppressWarnings("unchecked") private List<Map<String,Object>>mapList(Object v){return v instanceof List<?>l?(List<Map<String,Object>>)(List<?>)l:List.of();}
    private LocalDateTime date(Object v){if(v==null)return null;if(v instanceof Timestamp t)return t.toLocalDateTime();if(v instanceof LocalDateTime t)return t;return null;}
}
