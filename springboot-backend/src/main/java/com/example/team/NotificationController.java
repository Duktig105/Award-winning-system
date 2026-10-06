package com.example.team;

import com.example.auth.AuthContext;
import com.example.common.Result;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final JdbcTemplate jdbc;
    public NotificationController(JdbcTemplate jdbc){this.jdbc=jdbc;}
    @GetMapping public Result list(@RequestParam(defaultValue="1")int page,@RequestParam(defaultValue="20")int pageSize){
        int userId=AuthContext.require().userId();int offset=(Math.max(page,1)-1)*Math.min(Math.max(pageSize,1),50);
        return Result.success(java.util.Map.of(
            "list",jdbc.queryForList("SELECT notification_id notificationId,type,title,content,link,is_read isRead,create_time createTime FROM system_notification WHERE user_id=? ORDER BY create_time DESC LIMIT ?,?",userId,offset,pageSize),
            "unread",jdbc.queryForObject("SELECT COUNT(*) FROM system_notification WHERE user_id=? AND is_read=0",Integer.class,userId)));
    }
    @GetMapping("/unread-count") public Result unread(){return Result.success(jdbc.queryForObject("SELECT COUNT(*) FROM system_notification WHERE user_id=? AND is_read=0",Integer.class,AuthContext.require().userId()));}
    @PostMapping("/{id}/read") public Result read(@PathVariable int id){jdbc.update("UPDATE system_notification SET is_read=1 WHERE notification_id=? AND user_id=?",id,AuthContext.require().userId());return Result.success();}
    @PostMapping("/read-all") public Result readAll(){jdbc.update("UPDATE system_notification SET is_read=1 WHERE user_id=?",AuthContext.require().userId());return Result.success();}
}
