package com.example.ctreview.dto;

import com.example.ctreview.entity.ProblemStatus;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public record ProblemSearchRequest(
        Integer number,
        String q,                      // name contains
        Integer level,                 // currentLevel
        ProblemStatus status,          // ACTIVE/GRADUATED
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
        String sort                    // e.g., dateAsc, dateDesc, levelDesc
) {}
