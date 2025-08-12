package com.example.ctreview.dto;

import com.example.ctreview.entity.ProblemCategory;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record ProblemCreateRequest(
        Integer number,
        @NotBlank String name,
        ProblemCategory category,
        @Min(1) @Max(3) int level) {
}
