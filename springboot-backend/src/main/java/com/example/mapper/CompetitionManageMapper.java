package com.example.mapper;

import com.example.entity.Competition;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CompetitionManageMapper {

    /**
     * 查询竞赛总数
     */
    int countCompetitions(@Param("competitionName") String competitionName, @Param("awardRank") String awardRank);

    /**
     * 根据等次查询竞赛数
     */
    int countByAwardRank(@Param("awardRank") String awardRank);

    /**
     * 分页查询竞赛列表
     */
    List<Competition> selectCompetitionList(
            @Param("competitionName") String competitionName,
            @Param("awardRank") String awardRank,
            @Param("offset") Integer offset,
            @Param("pageSize") Integer pageSize);

    /**
     * 根据ID查询竞赛
     */
    Competition selectCompetitionById(@Param("competitionId") Integer competitionId);

    /**
     * 插入竞赛
     */
    int insertCompetition(Competition competition);

    /**
     * 更新竞赛
     */
    int updateCompetition(Competition competition);

    /**
     * 删除竞赛
     */
    int deleteCompetition(@Param("competitionId") Integer competitionId);

    /**
     * 批量删除竞赛
     */
    int batchDeleteCompetitions(@Param("ids") List<Integer> ids);

    /**
     * 查询所有竞赛（用于导出）
     */
    List<Competition> selectAllCompetitions(@Param("competitionName") String competitionName);
}
