package com.example.ctreview.controller;

import com.example.ctreview.entity.Problem;
import com.example.ctreview.entity.ProblemStatus;
import com.example.ctreview.repository.ProblemRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RolloverJob {

    private final ProblemRepository problemRepo;
    private final Clock clock;

    @Transactional
    @Scheduled(cron = "0 5 0 * * *", zone = "Asia/Seoul") // 매일 00:05 KST
    public void rolloverOverdue() {
        LocalDate today = LocalDate.now(clock);
        List<Problem> overdue = problemRepo.findByStatusAndNextReviewDateBefore(ProblemStatus.ACTIVE, today);
        for (Problem p : overdue) {
            long missed = ChronoUnit.DAYS.between(p.getNextReviewDate(), today);
            if (missed > 0) {
                p.setNextReviewDate(p.getNextReviewDate().plusDays(missed));
            }
        }
    }
}