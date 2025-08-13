package com.example.ctreview.controller;

import com.example.ctreview.entity.Problem;
import com.example.ctreview.entity.ProblemStatus;
import com.example.ctreview.repository.ProblemRepository;
import com.example.ctreview.entity.ReviewPolicy;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RolloverJob {

    private final ProblemRepository problemRepo;
    private final Clock clock;
    private final ReviewPolicy reviewPolicy;

    @Transactional
    @Scheduled(cron = "0 5 0 * * *", zone = "Asia/Seoul") // 매일 00:05 KST
    public void rolloverOverdue() {
        LocalDateTime now = LocalDateTime.now(clock);
        List<Problem> overdue = problemRepo.findByStatusAndNextReviewDateBefore(ProblemStatus.ACTIVE, now);
        ChronoUnit unit = reviewPolicy.unit();
        for (Problem p : overdue) {
            long missed = unit.between(p.getNextReviewDate(), now);
            if (missed > 0) {
                p.setNextReviewDate(p.getNextReviewDate().plus(missed, unit));
            }
        }
    }
}