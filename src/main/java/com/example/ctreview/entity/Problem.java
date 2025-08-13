package com.example.ctreview.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Table(name = "problems", indexes = {
        @Index(name = "ix_next_review_status", columnList = "next_review_date,status")
})
@Getter @Entity
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Problem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = true)                 // 번호는 있을 수도/없을 수도
    private Integer number;

    @Column(nullable = false, length = 200)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = true, length = 20)    // ★ 카테고리 선택(옵션)
    private ProblemCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProblemDifficulty difficulty;

    @Column(nullable = false) private int reviewStep;
    @Column(nullable = false) private int reviewCount;
    private LocalDateTime nextReviewDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false) private ProblemStatus status;

    @ManyToOne
    private User user;

    @Version private Long version;

    public void graduate() {
        this.reviewStep = 0;
        this.nextReviewDate = null;
        this.status = ProblemStatus.GRADUATED;
        this.reviewCount = 0; // 깔끔하게 초기화
    }

}
