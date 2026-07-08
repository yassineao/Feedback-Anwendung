package com.gloyoo.backend.answer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class AnswerRequestDto {
    @NotBlank
    private String answer;

    @NotNull
    private UUID questionId;
}
