package com.gloyoo.backend.survey.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SurveyRequestDto(
        @NotBlank @Size(max = 255) String title
) {
}
