package com.example.ctreview.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "review_logs", uniqueConstraints = {
        // 하루 1회 중복 처리 방지: 같은 문제, 같은 날짜, 같은 액션은 1회만
        @UniqueConstraint(name = "ux_log_problem_date_action", columnNames = {"problem_id", "action_date", "action"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "problem_id")
    private Problem problem;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReviewAction action; // SOLVE / FAIL

    @Column(name = "action_date", nullable = false)
    private LocalDate actionDate; // KST 기준

    // 스냅샷(선택): 전/후 상태 저장
    private Integer beforeStep;
    private Integer beforeReviewCount;
    private Integer afterStep;
    private Integer afterReviewCount;
}
