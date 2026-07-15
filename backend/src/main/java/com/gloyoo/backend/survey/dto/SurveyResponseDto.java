package com.gloyoo.backend.survey.dto;

import java.util.UUID;

public record SurveyResponseDto(UUID id, String title, UUID ownerId) {
}
