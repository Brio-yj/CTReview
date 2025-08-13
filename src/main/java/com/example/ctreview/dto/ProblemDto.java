package com.example.ctreview.dto;

import com.example.ctreview.entity.Problem;
import lombok.Builder;

import java.time.LocalDateTime;
@Builder
public record ProblemDto(
        Integer number, String name, String category,
        String difficulty,
        int reviewStep, int reviewCount, LocalDateTime nextReviewDate, String status
) {
    public static ProblemDto from(Problem p) {
        return ProblemDto.builder()
                .number(p.getNumber()).name(p.getName())
                .category(p.getCategory()==null?null:p.getCategory().name())
                .difficulty(p.getDifficulty().name())
                .reviewStep(p.getReviewStep())
                .reviewCount(p.getReviewCount())
                .nextReviewDate(p.getNextReviewDate())
                .status(p.getStatus().name())
                .build();
    }
}