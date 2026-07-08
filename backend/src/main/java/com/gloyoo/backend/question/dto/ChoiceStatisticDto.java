package com.gloyoo.backend.question.dto;

public record ChoiceStatisticDto(
        int choice,
        String label,
        long count,
        double respondentPercentage
) {
}
