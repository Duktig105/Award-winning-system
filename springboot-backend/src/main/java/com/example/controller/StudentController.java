package com.example.controller;

import com.example.common.Result;
import com.example.entity.Student;
import com.example.service.StudentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
@RequestMapping("/api/student")
public class StudentController {

    /**
     * 改动1：新增日志记录器。原代码所有 catch 分支都不写日志（如导入历史接口直接 body(error)），
     * 导致页面报错时 backend.log 中查不到任何堆栈，只能靠手工复现定位。
     */
    private static final Logger log = LoggerFactory.getLogger(StudentController.class);

    /** 改动2：分页上限，防止超大 pageSize 一次性拖回全表数据 */
    private static final int MAX_PAGE_SIZE = 200;

    @Autowired
    private StudentService studentService;

    /**
     * 获取学生统计信息
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        try {
            Map<String, Object> statistics = studentService.getStatistics();
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * 获取所有班级集合
     */
    @GetMapping("/classes")
    public ResponseEntity<Map<String, Object>> getDistinctClasses() {
        try {
            List<String> classes = studentService.getDistinctClasses();
            Map<String, Object> result = new HashMap<>();
            result.put("data", classes);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * 分页查询学生列表
     */
    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> getStudentList(
            @RequestParam(required = false) String studentNumber,
            @RequestParam(required = false) String grade,
            @RequestParam(required = false) String className,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        try {
            Map<String, Object> result = studentService.getStudentList(studentNumber, grade, className, page, pageSize);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * 根据ID查询学生
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getStudentById(@PathVariable Integer id) {
        try {
            Student student = studentService.getStudentById(id);
            Map<String, Object> result = new HashMap<>();
            result.put("data", student);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * 新增学生
     */
    @PostMapping("/add")
    public ResponseEntity<Map<String, Object>> addStudent(@RequestBody Student student) {
        try {
            studentService.addStudent(student);
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
     * 更新学生
     */
    @PutMapping("/update")
    public ResponseEntity<Map<String, Object>> updateStudent(@RequestBody Student student) {
        try {
            studentService.updateStudent(student);
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
     * 删除学生
     */
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Map<String, Object>> deleteStudent(@PathVariable Integer id) {
        try {
            studentService.deleteStudent(id);
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
     * 批量删除学生
     */
    @DeleteMapping("/batch-delete")
    public ResponseEntity<Map<String, Object>> batchDeleteStudents(@RequestBody List<Integer> ids) {
        try {
            studentService.batchDeleteStudents(ids);
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
     * 导出学生数据
     */
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportStudents(
            @RequestParam(required = false) String studentNumber,
            @RequestParam(required = false) String grade,
            @RequestParam(required = false) String className) {
        try {
            byte[] excelData = studentService.exportStudents(studentNumber, grade, className);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            String filename = "学生列表.xlsx";
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
     * 导入学生数据（升级版：校验/去重/更新/跳过/错误记录/导入历史）
     */
    @PostMapping("/import")
    public Result importStudents(@RequestParam("file") MultipartFile file) {
        try {
            String operator = com.example.auth.AuthContext.require().username();
            Map<String, Object> result = studentService.importStudents(file, operator);
            // 改动3：返回 Result 包装（原为裸 Map）。前端 StudentImport.vue 按 res.code === '200' 判断，
            // 且需要 res.data.insertCount 等字段；裸 Map 会导致"导入成功却提示导入失败"。
            return Result.success("导入完成", result);
        } catch (Exception e) {
            log.error("学生导入失败, file={}", file == null ? null : file.getOriginalFilename(), e);
            // 改动4：业务校验失败（如缺少表头、格式错误）以 Result.error 返回，
            // 使前端能展示 e.getMessage() 的具体原因，而不是笼统的"导入失败，请检查文件格式"。
            return Result.error(e.getMessage() != null ? e.getMessage() : "导入失败，请检查文件格式");
        }
    }

    /**
     * 下载学生导入Excel模板
     */
    @GetMapping("/import/template")
    public ResponseEntity<byte[]> downloadImportTemplate() {
        try {
            byte[] data = studentService.generateImportTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            String filename = URLEncoder.encode("学生导入模板.xlsx", "UTF-8");
            headers.setContentDispositionFormData("attachment", filename);
            return new ResponseEntity<>(data, headers, HttpStatus.OK);
        } catch (Exception e) {
            // 改动6：模板下载失败补充日志（原先静默返回 500 空响应）
            log.error("下载学生导入模板失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    /**
     * 分页查询导入历史记录
     */
    @GetMapping("/import/records")
    public Result listImportRecords(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        try {
            // 改动5：分页参数规范化。原代码直接把 page 透传给 SQL 的 OFFSET，
            // page=0 时生成 OFFSET -10 被 MySQL 拒绝（表现为 500）。
            int safePage = (page == null || page < 1) ? 1 : page;
            int safePageSize = (pageSize == null || pageSize < 1) ? 20 : Math.min(pageSize, MAX_PAGE_SIZE);
            // 改动3：返回 Result 包装。前端按 res.code === '200' -> res.data.list/res.data.total 解析，
            // 原裸 Map 响应（HTTP 200 + {list,total}）没有 code 字段，导致必然走到错误分支并提示"加载导入历史失败"。
            return Result.success(studentService.listImportRecords(safePage, safePageSize));
        } catch (Exception e) {
            log.error("查询学生导入历史失败, page={}, pageSize={}", page, pageSize, e);
            return Result.error(e.getMessage() != null ? e.getMessage() : "查询导入历史失败");
        }
    }

    /**
     * 查询某次导入的明细（可按 action=insert/update/fail/skip 过滤）
     */
    @GetMapping("/import/records/{importId}")
    public Result getImportRecordDetail(
            @PathVariable Integer importId,
            @RequestParam(required = false) String action,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        try {
            // 改动5（同上）：分页参数规范化 + 改动3：Result 包装，前端按 res.data.list 渲染明细
            int safePage = (page == null || page < 1) ? 1 : page;
            int safePageSize = (pageSize == null || pageSize < 1) ? 20 : Math.min(pageSize, MAX_PAGE_SIZE);
            return Result.success(studentService.getImportRecordDetail(importId, action, safePage, safePageSize));
        } catch (Exception e) {
            log.error("查询学生导入明细失败, importId={}, action={}, page={}, pageSize={}",
                    importId, action, page, pageSize, e);
            return Result.error(e.getMessage() != null ? e.getMessage() : "查询导入明细失败");
        }
    }

    /**
     * 下载某次导入的错误数据（Excel）
     */
    @GetMapping("/import/records/{importId}/errors")
    public ResponseEntity<byte[]> exportImportErrors(@PathVariable Integer importId) {
        try {
            byte[] data = studentService.exportImportErrors(importId);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            String filename = URLEncoder.encode("导入失败数据.xlsx", "UTF-8");
            headers.setContentDispositionFormData("attachment", filename);
            return new ResponseEntity<>(data, headers, HttpStatus.OK);
        } catch (Exception e) {
            // 改动6（同上）：错误数据导出失败补充日志
            log.error("导出学生导入错误数据失败, importId={}", importId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    /**
     * 重置学生密码
     */
    @PostMapping("/reset-password")
    public ResponseEntity<Boolean> resetPassword(@RequestBody Map<String, Object> request) {
        try {
            Integer studentId = (Integer) request.get("studentId");
            String studentNumber = (String) request.get("studentNumber");

            if (studentId == null || studentNumber == null || studentNumber.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(false);
            }

            boolean result = studentService.resetPassword(studentId, studentNumber);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(false);
        }
    }
}
