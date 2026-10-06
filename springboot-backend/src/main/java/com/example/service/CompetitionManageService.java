package com.example.service;

import com.example.entity.Competition;
import com.example.mapper.CompetitionManageMapper;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CompetitionManageService {

    @Autowired
    private CompetitionManageMapper competitionManageMapper;

    /**
     * 获取竞赛统计信息
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> statistics = new HashMap<>();
        int totalCount = competitionManageMapper.countCompetitions(null, null);
        int levelACount = competitionManageMapper.countByAwardRank("A");
        int levelBCount = competitionManageMapper.countByAwardRank("B");
        int levelCCount = competitionManageMapper.countByAwardRank("C");
        int levelDCount = competitionManageMapper.countByAwardRank("D");

        statistics.put("totalCount", totalCount);
        statistics.put("levelACount", levelACount);
        statistics.put("levelBCount", levelBCount);
        statistics.put("levelCCount", levelCCount);
        statistics.put("levelDCount", levelDCount);
        return statistics;
    }

    /**
     * 分页查询竞赛列表
     */
    public Map<String, Object> getCompetitionList(String competitionName, String awardRank, Integer page,
            Integer pageSize) {
        int offset = (page - 1) * pageSize;

        List<Competition> list = competitionManageMapper.selectCompetitionList(competitionName, awardRank, offset,
                pageSize);
        int total = competitionManageMapper.countCompetitions(competitionName, awardRank);

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total);
        result.put("page", page);
        result.put("pageSize", pageSize);

        return result;
    }

    /**
     * 根据ID查询竞赛
     */
    public Competition getCompetitionById(Integer competitionId) {
        return competitionManageMapper.selectCompetitionById(competitionId);
    }

    /**
     * 新增竞赛
     */
    public void addCompetition(Competition competition) throws Exception {
        if (competition.getCompetitionName() == null || competition.getCompetitionName().trim().isEmpty()) {
            throw new Exception("竞赛名称不能为空");
        }
        if (competition.getAwardRank() == null || competition.getAwardRank().trim().isEmpty()) {
            throw new Exception("获奖等次不能为空");
        }

        normalizeTeamRules(competition);
        competitionManageMapper.insertCompetition(competition);
    }

    /**
     * 更新竞赛
     */
    public void updateCompetition(Competition competition) throws Exception {
        if (competition.getCompetitionId() == null) {
            throw new Exception("竞赛ID不能为空");
        }
        if (competition.getCompetitionName() == null || competition.getCompetitionName().trim().isEmpty()) {
            throw new Exception("竞赛名称不能为空");
        }
        if (competition.getAwardRank() == null || competition.getAwardRank().trim().isEmpty()) {
            throw new Exception("获奖等次不能为空");
        }

        normalizeTeamRules(competition);
        competitionManageMapper.updateCompetition(competition);
    }

    /**
     * 删除竞赛
     */
    public void deleteCompetition(Integer competitionId) throws Exception {
        if (competitionId == null) {
            throw new Exception("竞赛ID不能为空");
        }

        competitionManageMapper.deleteCompetition(competitionId);
    }

    /**
     * 批量删除竞赛
     */
    public void batchDeleteCompetitions(List<Integer> ids) throws Exception {
        if (ids == null || ids.isEmpty()) {
            throw new Exception("请选择要删除的竞赛");
        }

        competitionManageMapper.batchDeleteCompetitions(ids);
    }

    /**
     * 导出竞赛数据为Excel
     */
    public byte[] exportCompetitions(String competitionName) throws IOException {
        List<Competition> competitions = competitionManageMapper.selectAllCompetitions(competitionName);

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("竞赛列表");

        // 创建表头
        Row headerRow = sheet.createRow(0);
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);

        String[] headers = { "序号", "竞赛名称", "获奖等次" };
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // 填充数据
        int rowNum = 1;
        for (Competition competition : competitions) {
            Row row = sheet.createRow(rowNum);
            row.createCell(0).setCellValue(rowNum); // 序号从1开始
            row.createCell(1).setCellValue(competition.getCompetitionName());
            row.createCell(2).setCellValue(competition.getAwardRank());
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
     * 导入竞赛数据从Excel
     */
    public Map<String, Object> importCompetitions(MultipartFile file) throws Exception {
        if (file.isEmpty()) {
            throw new Exception("上传文件不能为空");
        }

        List<Competition> successList = new ArrayList<>();
        List<String> errorList = new ArrayList<>();

        try (InputStream inputStream = file.getInputStream();
                Workbook workbook = new XSSFWorkbook(inputStream)) {

            Sheet sheet = workbook.getSheetAt(0);
            int rowCount = sheet.getPhysicalNumberOfRows();

            for (int i = 1; i < rowCount; i++) {
                Row row = sheet.getRow(i);
                if (row == null)
                    continue;

                try {
                    // 第0列是序号，从第1列开始读取数据
                    Cell nameCell = row.getCell(1);
                    Cell rankCell = row.getCell(2);

                    if (nameCell == null || rankCell == null) {
                        errorList.add("第" + (i + 1) + "行：数据不完整");
                        continue;
                    }

                    String competitionName = getCellValueAsString(nameCell);
                    String awardRank = getCellValueAsString(rankCell);

                    if (competitionName.trim().isEmpty() || awardRank.trim().isEmpty()) {
                        errorList.add("第" + (i + 1) + "行：竞赛名称或获奖等次为空");
                        continue;
                    }

                    Competition competition = new Competition();
                    competition.setCompetitionName(competitionName);
                    competition.setAwardRank(awardRank);

                    competitionManageMapper.insertCompetition(competition);
                    successList.add(competition);

                } catch (Exception e) {
                    errorList.add("第" + (i + 1) + "行：" + e.getMessage());
                }
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("successCount", successList.size());
        result.put("errorCount", errorList.size());
        result.put("errors", errorList);

        return result;
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

    private void normalizeTeamRules(Competition competition) throws Exception {
        if (competition.getCompetitionType() == null) competition.setCompetitionType("团体赛");
        if (competition.getMinTeamSize() == null) competition.setMinTeamSize(1);
        if (competition.getMaxTeamSize() == null) competition.setMaxTeamSize(10);
        if (competition.getTeamEnabled() == null) competition.setTeamEnabled(1);
        if (competition.getMinTeamSize() < 1 || competition.getMaxTeamSize() < competition.getMinTeamSize()) {
            throw new Exception("组队人数范围无效");
        }
        if (competition.getTeamOpenTime() != null && competition.getTeamCloseTime() != null
                && !competition.getTeamOpenTime().isBefore(competition.getTeamCloseTime())) {
            throw new Exception("组队截止时间必须晚于开放时间");
        }
    }
}
