package com.gloyoo.backend.question.dto;

import com.gloyoo.backend.question.entity.QuestionType;

import java.util.List;
import java.util.UUID;

public record QuestionStatisticsDto(
        UUID questionId,
        QuestionType type,
        long totalAnswers,
        List<ChoiceStatisticDto> choices
) {
}
