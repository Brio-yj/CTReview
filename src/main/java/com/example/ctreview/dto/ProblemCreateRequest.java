package com.example.ctreview.dto;

import com.example.ctreview.entity.ProblemCategory;
import com.example.ctreview.entity.ProblemDifficulty;
import jakarta.validation.constraints.NotBlank;

public record ProblemCreateRequest(
        Integer number,
        @NotBlank String name,
        ProblemCategory category,
        ProblemDifficulty difficulty) {
}
