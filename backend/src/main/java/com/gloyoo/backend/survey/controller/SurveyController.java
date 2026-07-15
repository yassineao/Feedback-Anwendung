package com.gloyoo.backend.survey.controller;

import com.gloyoo.backend.configuration.AuthenticatedUser;
import com.gloyoo.backend.survey.dto.SurveyRequestDto;
import com.gloyoo.backend.survey.dto.SurveyResponseDto;
import com.gloyoo.backend.survey.service.SurveyService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/surveys")
public class SurveyController {
    private final SurveyService surveyService;

    public SurveyController(SurveyService surveyService) {
        this.surveyService = surveyService;
    }

    @PostMapping
    public ResponseEntity<SurveyResponseDto> create(
            @Valid @RequestBody SurveyRequestDto request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(surveyService.create(request, user.id()));
    }

    @GetMapping
    public List<SurveyResponseDto> getOwnedSurveys(
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return surveyService.getOwnedSurveys(user.id());
    }

    @GetMapping("/{id}")
    public SurveyResponseDto getById(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return surveyService.getById(id, user.id());
    }
}
