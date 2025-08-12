package com.example.ctreview.dto;
import lombok.Builder;

import java.util.List;
import java.util.Map;

@Builder
public record DashboardSummaryDto(
        String today,
        int streak,
        List<DailyPoint> daily,              // 최근 30일 처리량
        Map<Integer, Long> levelDistribution,
        List<DailyPoint> graduations,        // 최근 30일 졸업 추이
        List<DailyPoint> heatmap             // 히트맵 소스(전체 기록)
) {
    @Builder
    public record DailyPoint(String date, long count) {}
}