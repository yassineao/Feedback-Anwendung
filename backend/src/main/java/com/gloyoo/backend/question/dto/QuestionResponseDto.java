package com.gloyoo.backend.question.dto;

import com.gloyoo.backend.question.entity.QuestionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionResponseDto {
    private UUID id;
    private String question;
    private QuestionType type;
    private UUID surveyId;
}
