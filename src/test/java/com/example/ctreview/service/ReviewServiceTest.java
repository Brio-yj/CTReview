package com.example.ctreview.service;

import com.example.ctreview.entity.Problem;
import com.example.ctreview.entity.ProblemDifficulty;
import com.example.ctreview.entity.ProblemStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class ReviewServiceTest {

    @TestConfiguration
    static class FixedClockConfig {
        @Bean
        public Clock fixedClock() {
            return Clock.fixed(Instant.parse("2024-01-01T00:00:00Z"), ZoneId.of("Asia/Seoul"));
        }
    }

    @Autowired
    ReviewService reviewService;

    @Autowired
    Clock clock;

    private long daysUntil(Problem p) {
        return Duration.between(LocalDateTime.now(clock), p.getNextReviewDate()).toDays();
    }

    @Test
    void highDifficultySolveFlow() {
        Problem p = reviewService.createProblem(null, null, "p1", null, ProblemDifficulty.HIGH);
        assertEquals(1, p.getReviewStep());
        assertEquals(0, p.getReviewCount());
        assertEquals(1, daysUntil(p));

        p = reviewService.solve(null, "p1");
        assertEquals(1, p.getReviewStep());
        assertEquals(1, p.getReviewCount());
        assertEquals(3, daysUntil(p));

        p = reviewService.solve(null, "p1");
        assertEquals(2, p.getReviewStep());
        assertEquals(2, p.getReviewCount());
        assertEquals(7, daysUntil(p));

        p = reviewService.solve(null, "p1");
        assertEquals(2, p.getReviewStep());
        assertEquals(3, p.getReviewCount());
        assertEquals(21, daysUntil(p));

        p = reviewService.solve(null, "p1");
        assertEquals(ProblemStatus.GRADUATED, p.getStatus());
        assertNull(p.getNextReviewDate());
    }

    @Test
    void failKeepsSameInterval() {
        Problem p = reviewService.createProblem(null, null, "m1", null, ProblemDifficulty.MEDIUM);
        p = reviewService.solve(null, "m1"); // -> 7 days
        p = reviewService.solve(null, "m1"); // -> 21 days
        assertEquals(2, p.getReviewCount());
        long before = daysUntil(p);

        p = reviewService.fail(null, "m1");
        assertEquals(2, p.getReviewCount());
        assertEquals(2, p.getReviewStep());
        assertEquals(before, daysUntil(p));
    }
}
