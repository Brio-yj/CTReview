package com.example.ctreview.entity;

import org.springframework.stereotype.Component;

import java.time.temporal.ChronoUnit;

/**
 * 난이도 기반으로 고정된 복습 간격과 단계를 제공하는 정책.
 */
@Component
public class DifficultyReviewPolicy implements ReviewPolicy {

    @Override
    public int[] intervals(ProblemDifficulty difficulty) {
        return switch (difficulty) {
            case HIGH -> new int[]{1, 3, 7, 21};
            case MEDIUM -> new int[]{3, 7, 21};
            case LOW -> new int[]{7, 21};
        };
    }

    @Override
    public int step(ProblemDifficulty difficulty, int index) {
        return switch (difficulty) {
            case HIGH -> (index <= 1) ? 1 : 2;
            case MEDIUM -> (index == 0) ? 1 : 2;
            case LOW -> (index == 0) ? 1 : 2;
        };
    }

    @Override
    public ChronoUnit unit() {
        return ChronoUnit.DAYS;
    }
}
