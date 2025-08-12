package com.example.ctreview.repository;

import com.example.ctreview.entity.Problem;
import com.example.ctreview.entity.ReviewAction;
import com.example.ctreview.entity.ReviewLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface ReviewLogRepository extends JpaRepository<ReviewLog, Long> {
    boolean existsByProblemAndActionDateAndAction(Problem problem, LocalDate date, ReviewAction action);

    List<ReviewLog> findByActionDateBetween(LocalDate from, LocalDate to);


}
