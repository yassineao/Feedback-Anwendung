package com.gloyoo.backend.survey.service;

import com.gloyoo.backend.survey.dto.SurveyRequestDto;
import com.gloyoo.backend.survey.dto.SurveyResponseDto;
import com.gloyoo.backend.survey.entity.Survey;
import com.gloyoo.backend.survey.repository.SurveyRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class SurveyService {
    private final SurveyRepository surveyRepository;

    public SurveyService(SurveyRepository surveyRepository) {
        this.surveyRepository = surveyRepository;
    }

    public SurveyResponseDto create(SurveyRequestDto request, UUID ownerId) {
        Survey survey = Survey.builder()
                .title(request.title().trim())
                .ownerId(ownerId)
                .build();
        return toResponse(surveyRepository.save(survey));
    }

    public List<SurveyResponseDto> getOwnedSurveys(UUID ownerId) {
        return surveyRepository.findAllByOwnerIdOrderByTitleAsc(ownerId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public SurveyResponseDto getById(UUID id, UUID ownerId) {
        return toResponse(findOwnedSurvey(id, ownerId));
    }

    public Survey findOwnedSurvey(UUID id, UUID ownerId) {
        Survey survey = surveyRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Survey not found with id: " + id));
        if (!survey.getOwnerId().equals(ownerId)) {
            throw new EntityNotFoundException("Survey not found with id: " + id);
        }
        return survey;
    }

    private SurveyResponseDto toResponse(Survey survey) {
        return new SurveyResponseDto(survey.getId(), survey.getTitle(), survey.getOwnerId());
    }

}
