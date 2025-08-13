package com.example.ctreview.controller;

import com.example.ctreview.dto.DashboardSummaryDto;
import com.example.ctreview.dto.ProblemDto;
import com.example.ctreview.entity.*;
import com.example.ctreview.repository.ProblemRepository;
import com.example.ctreview.repository.ReviewLogRepository;
import com.example.ctreview.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpSession;
import java.util.Objects;

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
    private final AuthService authService;

    @GetMapping("/summary")
    public DashboardSummaryDto summary(HttpSession session) {
        User user = authService.getCurrentUser(session);
        LocalDate today = LocalDate.now(clock);
        LocalDate from = today.minusDays(29);

        // streak
        int streak = 0;
        LocalDate d = today;
        while (true) {
            var logs = logRepo.findByActionDateBetween(d, d).stream()
                    .filter(l -> Objects.equals(l.getProblem().getUser(), user))
                    .toList();
            if (logs.isEmpty()) break;
            streak++;
            d = d.minusDays(1);
        }

        // daily (최근 30일)
        var recentLogs = logRepo.findByActionDateBetween(from, today).stream()
                .filter(l -> Objects.equals(l.getProblem().getUser(), user))
                .toList();
        Map<LocalDate, Long> dailyMap = recentLogs.stream()
                .collect(Collectors.groupingBy(l -> l.getActionDate(), Collectors.counting()));
        List<DashboardSummaryDto.DailyPoint> daily = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            LocalDate day = from.plusDays(i);
            daily.add(new DashboardSummaryDto.DailyPoint(day.toString(), dailyMap.getOrDefault(day, 0L)));
        }

        // stepDistribution
        Map<Integer, Long> stepDist = problemRepo.findByStatus(ProblemStatus.ACTIVE).stream()
                .collect(Collectors.groupingBy(Problem::getReviewStep, Collectors.counting()));

        // graduations (Solve로 1→0)


        Map<LocalDate, Long> gradMap = recentLogs.stream()
                .filter(l -> l.getAction() == ReviewAction.SOLVE
                        && l.getBeforeStep() != null && l.getBeforeStep() == 3
                        && l.getAfterStep() != null && l.getAfterStep() == 0)
                .collect(Collectors.groupingBy(ReviewLog::getActionDate, Collectors.counting()));
        List<DashboardSummaryDto.DailyPoint> graduations = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            LocalDate day = from.plusDays(i);
            graduations.add(new DashboardSummaryDto.DailyPoint(day.toString(), gradMap.getOrDefault(day, 0L)));
        }


        // heatmap: 전체 기록
        var allLogs = logRepo.findAll().stream()
                .filter(l -> Objects.equals(l.getProblem().getUser(), user))
                .toList();
        Map<LocalDate, Long> heatMap = allLogs.stream()
                .collect(Collectors.groupingBy(ReviewLog::getActionDate, Collectors.counting()));
        LocalDate heatFrom = allLogs.stream()
                .map(ReviewLog::getActionDate)
                .min(LocalDate::compareTo)
                .orElse(today);


        Map<String, Long> gradByDiff = problemRepo.findByStatus(ProblemStatus.GRADUATED).stream()
                .collect(Collectors.groupingBy(p -> p.getDifficulty().name(), Collectors.counting()));

        var graduatedProblems = problemRepo.findByStatus(ProblemStatus.GRADUATED).stream()
                .map(ProblemDto::from)
                .toList();
        List<DashboardSummaryDto.DailyPoint> heat = new ArrayList<>();
        for (LocalDate day = heatFrom; !day.isAfter(today); day = day.plusDays(1)) {
            heat.add(new DashboardSummaryDto.DailyPoint(day.toString(), heatMap.getOrDefault(day, 0L)));
        }

        return DashboardSummaryDto.builder()
                .today(today.toString())
                .streak(streak)
                .daily(daily)
                .stepDistribution(stepDist)
                .graduationByDifficulty(gradByDiff)

                .graduations(graduations)

                .graduatedProblems(graduatedProblems)
                .heatmap(heat)
                .build();
    }
}

