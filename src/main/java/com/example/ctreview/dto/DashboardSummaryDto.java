package com.example.ctreview.dto;
import lombok.Builder;

import java.util.List;
import java.util.Map;

@Builder
public record DashboardSummaryDto(
        String today,
        int streak,
        List<DailyPoint> daily,
        Map<Integer, Long> stepDistribution,
        Map<String, Long> graduationByDifficulty,

        List<DailyPoint> graduations,

        List<ProblemDto> graduatedProblems,
        List<DailyPoint> heatmap
) {
    @Builder
    public record DailyPoint(String date, long count) {}
}