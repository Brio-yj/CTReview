package com.example.ctreview.controller;

import com.example.ctreview.dto.DashboardSummaryDto;
import com.example.ctreview.entity.*;
import com.example.ctreview.repository.ProblemRepository;
import com.example.ctreview.repository.ReviewLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final ReviewLogRepository logRepo;
    private final ProblemRepository problemRepo;
    private final Clock clock;

    @GetMapping("/summary")
    public DashboardSummaryDto summary() {
        LocalDate today = LocalDate.now(clock);
        LocalDate from = today.minusDays(29);

        // streak
        int streak = 0;
        LocalDate d = today;
        while (true) {
            var logs = logRepo.findByActionDateBetween(d, d);
            if (logs.isEmpty()) break;
            streak++;
            d = d.minusDays(1);
        }

        // daily (최근 30일)
        var recentLogs = logRepo.findByActionDateBetween(from, today);
        Map<LocalDate, Long> dailyMap = recentLogs.stream()
                .collect(Collectors.groupingBy(l -> l.getActionDate(), Collectors.counting()));
        List<DashboardSummaryDto.DailyPoint> daily = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            LocalDate day = from.plusDays(i);
            daily.add(new DashboardSummaryDto.DailyPoint(day.toString(), dailyMap.getOrDefault(day, 0L)));
        }

        // levelDistribution
        Map<Integer, Long> levelDist = problemRepo.findByStatus(ProblemStatus.ACTIVE).stream()
                .collect(Collectors.groupingBy(Problem::getCurrentLevel, Collectors.counting()));

        // graduations (Solve로 1→0)
        Map<LocalDate, Long> gradMap = recentLogs.stream()
                .filter(l -> l.getAction() == ReviewAction.SOLVE
                        && l.getBeforeLevel() != null && l.getBeforeLevel() == 1
                        && l.getAfterLevel() != null && l.getAfterLevel() == 0)
                .collect(Collectors.groupingBy(ReviewLog::getActionDate, Collectors.counting()));
        List<DashboardSummaryDto.DailyPoint> graduations = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            LocalDate day = from.plusDays(i);
            graduations.add(new DashboardSummaryDto.DailyPoint(day.toString(), gradMap.getOrDefault(day, 0L)));
        }

        // heatmap 소스: 최근 12주 = 84일(대략)
        LocalDate heatFrom = today.minusDays(83);
        var heatLogs = logRepo.findByActionDateBetween(heatFrom, today);
        Map<LocalDate, Long> heatMap = heatLogs.stream()
                .collect(Collectors.groupingBy(ReviewLog::getActionDate, Collectors.counting()));
        List<DashboardSummaryDto.DailyPoint> heat = new ArrayList<>();
        for (int i = 0; i <= 83; i++) {
            LocalDate day = heatFrom.plusDays(i);
            heat.add(new DashboardSummaryDto.DailyPoint(day.toString(), heatMap.getOrDefault(day, 0L)));
        }

        return DashboardSummaryDto.builder()
                .today(today.toString())
                .streak(streak)
                .daily(daily)
                .levelDistribution(levelDist)
                .graduations(graduations)
                .heatmap(heat)
                .build();
    }
}

