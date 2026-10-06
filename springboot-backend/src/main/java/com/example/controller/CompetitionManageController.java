package com.example.controller;

import com.example.entity.Competition;
import com.example.service.CompetitionManageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/competition-manage")
public class CompetitionManageController {

    @Autowired
    private CompetitionManageService competitionManageService;

    /**
     * 获取竞赛统计信息
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        try {
            Map<String, Object> statistics = competitionManageService.getStatistics();
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * 分页查询竞赛列表
     */
    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> getCompetitionList(
            @RequestParam(required = false) String competitionName,
            @RequestParam(required = false) String awardRank,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        try {
            Map<String, Object> result = competitionManageService.getCompetitionList(competitionName, awardRank, page,
                    pageSize);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * 根据ID查询竞赛
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getCompetitionById(@PathVariable Integer id) {
        try {
            Competition competition = competitionManageService.getCompetitionById(id);
            Map<String, Object> result = new HashMap<>();
            result.put("data", competition);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * 新增竞赛
     */
    @PostMapping("/add")
    public ResponseEntity<Map<String, Object>> addCompetition(@RequestBody Competition competition) {
        try {
            competitionManageService.addCompetition(competition);
            Map<String, Object> result = new HashMap<>();
            result.put("message", "新增成功");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * 更新竞赛
     */
    @PutMapping("/update")
    public ResponseEntity<Map<String, Object>> updateCompetition(@RequestBody Competition competition) {
        try {
            competitionManageService.updateCompetition(competition);
            Map<String, Object> result = new HashMap<>();
            result.put("message", "更新成功");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * 删除竞赛
     */
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Map<String, Object>> deleteCompetition(@PathVariable Integer id) {
        try {
            competitionManageService.deleteCompetition(id);
            Map<String, Object> result = new HashMap<>();
            result.put("message", "删除成功");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * 批量删除竞赛
     */
    @DeleteMapping("/batch-delete")
    public ResponseEntity<Map<String, Object>> batchDeleteCompetitions(@RequestBody List<Integer> ids) {
        try {
            competitionManageService.batchDeleteCompetitions(ids);
            Map<String, Object> result = new HashMap<>();
            result.put("message", "批量删除成功");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * 导出竞赛数据
     */
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportCompetitions(@RequestParam(required = false) String competitionName) {
        try {
            byte[] excelData = competitionManageService.exportCompetitions(competitionName);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            String filename = "竞赛列表.xlsx";
            try {
                filename = URLEncoder.encode(filename, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            headers.setContentDispositionFormData("attachment", filename);

            return new ResponseEntity<>(excelData, headers, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    /**
     * 导入竞赛数据
     */
    @PostMapping("/import")
    public ResponseEntity<Map<String, Object>> importCompetitions(@RequestParam("file") MultipartFile file) {
        try {
            Map<String, Object> result = competitionManageService.importCompetitions(file);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }
}
