package com.gloyoo.backend.question.dto;

import com.gloyoo.backend.question.entity.QuestionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class QuestionRequestDto {
    @NotBlank
    private String question;

    @NotNull
    private QuestionType type;
}
