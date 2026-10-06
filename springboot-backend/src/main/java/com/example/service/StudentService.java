package com.example.service;

import com.example.entity.Student;
import com.example.mapper.StudentMapper;
import com.example.mapper.UserMapper;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class StudentService {

    @Autowired
    private StudentMapper studentMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private JdbcTemplate jdbc;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * 获取学生统计信息
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> statistics = new HashMap<>();
        int totalCount = studentMapper.countStudents(null, null, null);
        statistics.put("totalCount", totalCount);

        List<Map<String, Object>> gradeStats = studentMapper.countStudentsByGrade();
        statistics.put("gradeStats", gradeStats);

        return statistics;
    }

    /**
     * 分页查询学生列表
     */
    public Map<String, Object> getStudentList(String studentNumber, String grade, String className, Integer page,
            Integer pageSize) {
        int offset = (page - 1) * pageSize;

        List<Student> list = studentMapper.selectStudentList(studentNumber, grade, className, offset, pageSize);
        int total = studentMapper.countStudents(studentNumber, grade, className);

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total);
        result.put("page", page);
        result.put("pageSize", pageSize);

        return result;
    }

    /**
     * 根据ID查询学生
     */
    public Student getStudentById(Integer studentId) {
        return studentMapper.selectStudentById(studentId);
    }

    /**
     * 新增学生
     */
    public void addStudent(Student student) throws Exception {
        if (student.getStudentNumber() == null || student.getStudentNumber().trim().isEmpty()) {
            throw new Exception("学号不能为空");
        }
        if (student.getStudentName() == null || student.getStudentName().trim().isEmpty()) {
            throw new Exception("学生姓名不能为空");
        }
        if (student.getGrade() == null || student.getGrade().trim().isEmpty()) {
            throw new Exception("年级不能为空");
        }

        studentMapper.insertStudent(student);
    }

    /**
     * 更新学生
     */
    public void updateStudent(Student student) throws Exception {
        if (student.getStudentId() == null) {
            throw new Exception("学生ID不能为空");
        }
        if (student.getStudentNumber() == null || student.getStudentNumber().trim().isEmpty()) {
            throw new Exception("学号不能为空");
        }
        if (student.getStudentName() == null || student.getStudentName().trim().isEmpty()) {
            throw new Exception("学生姓名不能为空");
        }
        if (student.getGrade() == null || student.getGrade().trim().isEmpty()) {
            throw new Exception("年级不能为空");
        }

        studentMapper.updateStudent(student);
    }

    /**
     * 删除学生
     */
    public void deleteStudent(Integer studentId) throws Exception {
        if (studentId == null) {
            throw new Exception("学生ID不能为空");
        }

        studentMapper.deleteStudent(studentId);
    }

    /**
     * 批量删除学生
     */
    public void batchDeleteStudents(List<Integer> ids) throws Exception {
        if (ids == null || ids.isEmpty()) {
            throw new Exception("请选择要删除的学生");
        }

        studentMapper.batchDeleteStudents(ids);
    }

    /**
     * 查询所有去重班级
     */
    public List<String> getDistinctClasses() {
        return studentMapper.selectDistinctClasses();
    }

    /**
     * 导出学生数据为Excel
     */
    public byte[] exportStudents(String studentNumber, String grade, String className) throws IOException {
        List<Student> students = studentMapper.selectAllStudents(studentNumber, grade, className);

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("学生列表");

        // 创建表头
        Row headerRow = sheet.createRow(0);
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);

        String[] headers = { "序号", "学号", "姓名", "年级", "专业", "班级", "学院" };
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // 填充数据
        int rowNum = 1;
        for (Student student : students) {
            Row row = sheet.createRow(rowNum);
            row.createCell(0).setCellValue(rowNum); // 序号从1开始
            row.createCell(1).setCellValue(student.getStudentNumber());
            row.createCell(2).setCellValue(student.getStudentName());
            row.createCell(3).setCellValue(student.getGrade());
            row.createCell(4).setCellValue(student.getMajor());
            row.createCell(5).setCellValue(student.getClassName());
            row.createCell(6).setCellValue(student.getCollege());
            rowNum++;
        }

        // 自动调整列宽
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();

        return outputStream.toByteArray();
    }

    /**
     * 导入学生数据从Excel（升级版）
     * 支持：表头自适应（学号/姓名/年级/专业/班级/学院）、字段校验、文件内学号去重、
     * 已有学生更新、无变化跳过、错误数据记录与下载、导入记录留存。
     */
    @Transactional
    public Map<String, Object> importStudents(MultipartFile file, String operator) throws Exception {
        if (file.isEmpty()) {
            throw new Exception("上传文件不能为空");
        }
        String uploadName = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        if (!uploadName.endsWith(".xlsx")) {
            throw new Exception("仅支持 .xlsx 格式文件，请先将Excel另存为xlsx后重试");
        }

        int totalCount = 0, insertCount = 0, updateCount = 0, skipCount = 0, failCount = 0;
        List<Map<String, Object>> details = new ArrayList<>();
        Set<String> seenNumbers = new HashSet<>(); // 文件内学号重复检查
        String defaultCollege = detectDefaultCollege();

        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(inputStream)) {

            Sheet sheet = workbook.getSheetAt(0);
            if (sheet.getPhysicalNumberOfRows() < 1) {
                throw new Exception("Excel内容为空");
            }

            // 表头识别：优先按表头文字定位列，识别失败则按固定布局
            Map<String, Integer> colMap = detectHeader(sheet.getRow(0));
            int firstDataRow = colMap.isEmpty() ? 0 : 1;
            if (colMap.isEmpty()) {
                // 兼容旧格式：0-序号,1-学号,2-姓名,3-年级,4-专业,5-班级,6-学院
                Row first = sheet.getRow(0);
                int cols = first == null ? 0 : first.getLastCellNum();
                if (cols >= 7) {
                    colMap = Map.of("学号", 1, "姓名", 2, "年级", 3, "专业", 4, "班级", 5, "学院", 6);
                } else {
                    // 新格式无表头：0-学号,1-姓名,2-年级,3-专业,4-班级,5-学院
                    colMap = Map.of("学号", 0, "姓名", 1, "年级", 2, "专业", 3, "班级", 4, "学院", 5);
                }
            }

            int rowCount = sheet.getPhysicalNumberOfRows();
            for (int i = firstDataRow; i < rowCount; i++) {
                Row row = sheet.getRow(i);
                if (row == null || isBlankRow(row, colMap)) continue;
                totalCount++;

                String studentNumber = getCell(row, colMap.get("学号"));
                String studentName = getCell(row, colMap.get("姓名"));
                String grade = getCell(row, colMap.get("年级"));
                String major = getCell(row, colMap.get("专业"));
                String className = getCell(row, colMap.get("班级"));
                String college = getCell(row, colMap.get("学院"));

                Map<String, Object> detail = new LinkedHashMap<>();
                detail.put("rowNum", i + 1);
                detail.put("studentNumber", studentNumber);
                detail.put("studentName", studentName);
                detail.put("grade", grade);
                detail.put("major", major);
                detail.put("className", className);
                detail.put("college", college);

                // ---- 校验 ----
                String error = null;
                if (studentNumber.isEmpty() || studentName.isEmpty() || grade.isEmpty()) {
                    error = "学号、姓名、年级为必填项";
                } else if (!studentNumber.matches("[0-9A-Za-z\\-]{4,20}")) {
                    error = "学号格式不正确（4-20位数字/字母/中划线）";
                } else if (studentName.length() > 20) {
                    error = "姓名过长（最多20字）";
                } else if (grade.length() > 10) {
                    error = "年级过长";
                } else if (major.length() > 30) {
                    error = "专业过长（最多30字）";
                } else if (className.length() > 30) {
                    error = "班级过长（最多30字）";
                } else if (college.length() > 50) {
                    error = "学院过长（最多50字）";
                } else if (!seenNumbers.add(studentNumber)) {
                    error = "学号在文件中重复";
                }

                if (error != null) {
                    failCount++;
                    detail.put("action", "fail");
                    detail.put("message", error);
                    details.add(detail);
                    continue;
                }

                try {
                    // ---- 新增 或 更新 ----
                    List<Map<String, Object>> existList = jdbc.queryForList(
                            "SELECT student_id, student_number, student_name, grade, major, class_name, college " +
                                    "FROM student WHERE student_number = ?", studentNumber);
                    if (existList.isEmpty()) {
                        String finalCollege = college.isEmpty() ? defaultCollege : college;
                        jdbc.update("INSERT INTO student (student_number, student_name, grade, major, class_name, college) " +
                                        "VALUES (?, ?, ?, ?, ?, ?)",
                                studentNumber, studentName, grade, major, className, finalCollege);
                        insertCount++;
                        detail.put("action", "insert");
                        detail.put("message", "新增学生");
                    } else {
                        Map<String, Object> exist = existList.get(0);
                        String existCollege = exist.get("college") == null ? "" : String.valueOf(exist.get("college"));
                        boolean same = studentName.equals(str(exist.get("student_name")))
                                && grade.equals(str(exist.get("grade")))
                                && major.equals(str(exist.get("major")))
                                && className.equals(str(exist.get("class_name")))
                                && (college.isEmpty() || college.equals(existCollege));
                        if (same) {
                            skipCount++;
                            detail.put("action", "skip");
                            detail.put("message", "与已有数据一致，跳过");
                        } else {
                            // 学院为空时保留原值
                            String newCollege = college.isEmpty() ? existCollege : college;
                            jdbc.update("UPDATE student SET student_name = ?, grade = ?, major = ?, class_name = ?, college = ? " +
                                            "WHERE student_number = ?",
                                    studentName, grade, major, className, newCollege, studentNumber);
                            updateCount++;
                            detail.put("action", "update");
                            detail.put("message", "更新已有学生");
                        }
                    }
                } catch (Exception e) {
                    failCount++;
                    detail.put("action", "fail");
                    detail.put("message", "写入失败: " + e.getMessage());
                }
                details.add(detail);
            }
        }

        // ---- 保存导入记录 ----
        jdbc.update("INSERT INTO student_import_record (file_name, total_count, insert_count, update_count, " +
                        "skip_count, fail_count, operator, import_time) VALUES (?, ?, ?, ?, ?, ?, ?, NOW())",
                file.getOriginalFilename(), totalCount, insertCount, updateCount, skipCount, failCount, operator);
        Integer importId = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Integer.class);

        for (Map<String, Object> d : details) {
            jdbc.update("INSERT INTO student_import_detail (import_id, row_num, student_number, student_name, " +
                            "grade, major, class_name, college, action, message) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    importId, d.get("rowNum"), d.get("studentNumber"), d.get("studentName"), d.get("grade"),
                    d.get("major"), d.get("className"), d.get("college"), d.get("action"), d.get("message"));
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("importId", importId);
        result.put("totalCount", totalCount);
        result.put("insertCount", insertCount);
        result.put("updateCount", updateCount);
        result.put("skipCount", skipCount);
        result.put("failCount", failCount);
        result.put("failDetails", details.stream().filter(d -> "fail".equals(d.get("action"))).toList());
        return result;
    }

    /** 识别表头行，返回 列名 -> 列下标；无法识别返回空Map */
    private Map<String, Integer> detectHeader(Row headerRow) {
        Map<String, Integer> map = new HashMap<>();
        if (headerRow == null) return map;
        for (int c = 0; c < headerRow.getLastCellNum(); c++) {
            String text = getCellValueAsString(headerRow.getCell(c)).replaceAll("\\s+", "");
            switch (text) {
                case "学号" -> map.put("学号", c);
                case "姓名" -> map.put("姓名", c);
                case "年级" -> map.put("年级", c);
                case "专业" -> map.put("专业", c);
                case "班级" -> map.put("班级", c);
                case "学院" -> map.put("学院", c);
            }
        }
        // 至少要有学号+姓名才算识别成功
        if (!map.containsKey("学号") || !map.containsKey("姓名")) {
            return new HashMap<>();
        }
        return map;
    }

    private boolean isBlankRow(Row row, Map<String, Integer> colMap) {
        return getCell(row, colMap.get("学号")).isEmpty()
                && getCell(row, colMap.get("姓名")).isEmpty();
    }

    private String getCell(Row row, Integer col) {
        if (row == null || col == null) return "";
        return getCellValueAsString(row.getCell(col)).trim();
    }

    private String str(Object o) {
        return o == null ? "" : String.valueOf(o).trim();
    }

    /** 学院为空时的默认值：取现有学生中数量最多的学院 */
    private String detectDefaultCollege() {
        try {
            List<String> list = jdbc.queryForList(
                    "SELECT college FROM student WHERE college IS NOT NULL AND college <> '' " +
                            "GROUP BY college ORDER BY COUNT(*) DESC LIMIT 1", String.class);
            return list.isEmpty() ? "" : list.get(0);
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * 生成学生导入Excel模板
     */
    public byte[] generateImportTemplate() throws IOException {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("学生名单");
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            String[] headers = {"学号", "姓名", "年级", "专业", "班级", "学院（可选）"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }
            // 示例数据
            Row r1 = sheet.createRow(1);
            r1.createCell(0).setCellValue("202221121001");
            r1.createCell(1).setCellValue("张三");
            r1.createCell(2).setCellValue("22级");
            r1.createCell(3).setCellValue("生物医学工程");
            r1.createCell(4).setCellValue("生医2201班");
            r1.createCell(5).setCellValue("生物医学工程学院");
            Row r2 = sheet.createRow(2);
            r2.createCell(0).setCellValue("202321121001");
            r2.createCell(1).setCellValue("李四");
            r2.createCell(2).setCellValue("23级");
            r2.createCell(3).setCellValue("医学信息工程");
            r2.createCell(4).setCellValue("医学信息2301班");
            r2.createCell(5).setCellValue("生物医学工程学院");

            for (int i = 0; i < headers.length; i++) {
                sheet.setColumnWidth(i, 20 * 256);
            }
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    /**
     * 分页查询导入历史记录
     */
    public Map<String, Object> listImportRecords(int page, int pageSize) {
        int total = jdbc.queryForObject("SELECT COUNT(*) FROM student_import_record", Integer.class);
        List<Map<String, Object>> list = jdbc.queryForList(
                "SELECT import_id AS importId, file_name AS fileName, total_count AS totalCount, " +
                        "insert_count AS insertCount, update_count AS updateCount, skip_count AS skipCount, " +
                        "fail_count AS failCount, operator, import_time AS importTime " +
                        "FROM student_import_record ORDER BY import_id DESC LIMIT ? OFFSET ?",
                pageSize, (page - 1) * pageSize);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("list", list);
        result.put("total", total);
        result.put("page", page);
        result.put("pageSize", pageSize);
        return result;
    }

    /**
     * 查询某次导入的明细（分页，可按action过滤）
     */
    public Map<String, Object> getImportRecordDetail(int importId, String action, int page, int pageSize) {
        StringBuilder where = new StringBuilder(" WHERE import_id = ? ");
        List<Object> args = new ArrayList<>();
        args.add(importId);
        if (action != null && !action.isBlank()) {
            where.append(" AND action = ? ");
            args.add(action);
        }
        int total = jdbc.queryForObject("SELECT COUNT(*) FROM student_import_detail" + where,
                Integer.class, args.toArray());
        List<Object> queryArgs = new ArrayList<>(args);
        queryArgs.add(pageSize);
        queryArgs.add((page - 1) * pageSize);
        List<Map<String, Object>> list = jdbc.queryForList(
                "SELECT detail_id AS detailId, row_num AS rowNum, student_number AS studentNumber, " +
                        "student_name AS studentName, grade, major, class_name AS className, college, action, message " +
                        "FROM student_import_detail" + where + " ORDER BY detail_id LIMIT ? OFFSET ?",
                queryArgs.toArray(new Object[0]));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("list", list);
        result.put("total", total);
        result.put("page", page);
        result.put("pageSize", pageSize);
        return result;
    }

    /**
     * 导出某次导入的错误数据（Excel）
     */
    public byte[] exportImportErrors(int importId) throws IOException {
        List<Map<String, Object>> fails = jdbc.queryForList(
                "SELECT row_num AS rowNum, student_number AS studentNumber, student_name AS studentName, " +
                        "grade, major, class_name AS className, college, message " +
                        "FROM student_import_detail WHERE import_id = ? AND action = 'fail' ORDER BY row_num", importId);

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("导入失败数据");
            String[] headers = {"行号", "学号", "姓名", "年级", "专业", "班级", "学院", "失败原因"};
            Row headerRow = sheet.createRow(0);
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }
            int rowNum = 1;
            for (Map<String, Object> f : fails) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(f.get("rowNum") == null ? "" : String.valueOf(f.get("rowNum")));
                row.createCell(1).setCellValue(str(f.get("studentNumber")));
                row.createCell(2).setCellValue(str(f.get("studentName")));
                row.createCell(3).setCellValue(str(f.get("grade")));
                row.createCell(4).setCellValue(str(f.get("major")));
                row.createCell(5).setCellValue(str(f.get("className")));
                row.createCell(6).setCellValue(str(f.get("college")));
                row.createCell(7).setCellValue(str(f.get("message")));
            }
            for (int i = 0; i < headers.length; i++) {
                sheet.setColumnWidth(i, 20 * 256);
            }
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    /**
     * 重置学生密码
     */
    public boolean resetPassword(Integer studentId, String studentNumber) {
        try {
            // 新密码格式：SY + 学号
            String newPassword = "SY" + studentNumber;

            // 更新密码
            Student student = studentMapper.selectStudentById(studentId);
            if (student == null || !studentNumber.equals(student.getStudentNumber())) return false;
            userMapper.updatePasswordByUsername(studentNumber, passwordEncoder.encode(newPassword));
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 获取单元格值为字符串
     */
    private String getCellValueAsString(Cell cell) {
        if (cell == null)
            return "";

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                return String.valueOf((int) cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }
}
