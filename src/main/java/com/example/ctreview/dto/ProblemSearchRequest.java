package com.example.ctreview.dto;

import com.example.ctreview.entity.ProblemStatus;
import com.example.ctreview.entity.ProblemDifficulty;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public record ProblemSearchRequest(
        Integer number,
        String q,                      // name contains
        ProblemDifficulty difficulty,  // 난이도 필터
        ProblemStatus status,          // ACTIVE/GRADUATED
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
        String sort                    // e.g., dateAsc, dateDesc, stepDesc
) {}
