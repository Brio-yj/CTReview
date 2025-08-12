package com.example.ctreview.dto;

import com.example.ctreview.entity.Problem;
import lombok.Builder;

import java.time.LocalDate;
@Builder
public record ProblemDto(
        Integer number, String name, String category,
        int currentLevel, int reviewCount, LocalDate nextReviewDate, String status
) {
    public static ProblemDto from(Problem p) {
        return ProblemDto.builder()
                .number(p.getNumber()).name(p.getName())
                .category(p.getCategory()==null?null:p.getCategory().name())
                .currentLevel(p.getCurrentLevel())
                .reviewCount(p.getReviewCount())
                .nextReviewDate(p.getNextReviewDate())
                .status(p.getStatus().name())
                .build();
    }
}