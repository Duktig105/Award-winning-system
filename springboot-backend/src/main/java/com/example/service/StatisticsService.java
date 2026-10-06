package com.example.service;

import com.example.mapper.ApplicationMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class StatisticsService {

    @Autowired
    private ApplicationMapper applicationMapper;

    public Map<String, Object> getOverview() {
        Map<String, Object> result = new HashMap<>();

        // KPI
        Map<String, Object> kpi = new HashMap<>();
        kpi.put("totalCount", applicationMapper.countApplicationsByStatus(null));
        kpi.put("approvedCount", applicationMapper.countApplicationsByStatus("approved"));
        kpi.put("pendingCount", applicationMapper.countApplicationsByStatus("pending"));
        kpi.put("rejectedCount", applicationMapper.countApplicationsByStatus("rejected"));
        kpi.put("returnedCount", applicationMapper.countApplicationsByStatus("returned"));
        result.put("kpi", kpi);

        result.put("awardRankStats", applicationMapper.countApprovedByAwardRank());
        result.put("competitionLevelStats", applicationMapper.countApprovedByCompetitionLevel());
        result.put("gradeAwardStats", applicationMapper.countApprovedByGrade());
        result.put("statusStats", applicationMapper.countByAllStatus());
        result.put("top10Competitions", applicationMapper.selectTop10Competitions());
        result.put("monthlyTrend", applicationMapper.selectMonthlyTrend());

        return result;
    }
}
