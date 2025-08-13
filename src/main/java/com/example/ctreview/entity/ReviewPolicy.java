package com.example.ctreview.entity;

import java.time.temporal.ChronoUnit;

/**
 * 리뷰 정책 인터페이스.
 * 난이도에 따라 고정된 간격과 단계가 결정된다.
 */
public interface ReviewPolicy {

    /**
     * 주어진 난이도의 전체 복습 간격 목록을 반환한다.
     */
    int[] intervals(ProblemDifficulty difficulty);

    /**
     * 난이도와 인덱스를 기반으로 복습 단계를 반환한다.
     * (1 또는 2 단계만 존재)
     */
    int step(ProblemDifficulty difficulty, int index);

    /**
     * 간격에 사용되는 단위 (예: DAYS)
     */
    ChronoUnit unit();
}
