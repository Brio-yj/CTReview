package com.example.ctreview.repository;

import com.example.ctreview.entity.Problem;
import com.example.ctreview.entity.ProblemStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ProblemRepository extends JpaRepository<Problem, Long> {
    boolean existsByNumber(int number);
    Optional<Problem> findByNumber(int number);
    List<Problem> findByNextReviewDateAndStatusOrderByCurrentLevelDesc(LocalDate date, ProblemStatus status);
    List<Problem> findByStatusOrderByNextReviewDateAsc(ProblemStatus status);
    List<Problem> findByStatusAndNextReviewDateBefore(ProblemStatus status, LocalDate date);
    List<Problem> findByStatus(ProblemStatus status);
    void deleteByNumber(int number);

    boolean existsByName(String name);
    Optional<Problem> findByName(String name);

    List<Problem> findAllByNumber(Integer number);

    @Query("select coalesce(max(p.number), 0) from Problem p") // ★
    int findMaxNumber();
}

