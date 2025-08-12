package com.example.ctreview.dto;
import lombok.Builder;

@Builder
public record ActionResultDto(
        String message,
        ProblemDto problem
) {
    public static ActionResultDto of(String message, ProblemDto dto) {
        return ActionResultDto.builder().message(message).problem(dto).build();
    }
}
