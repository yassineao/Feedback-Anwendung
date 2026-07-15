package com.gloyoo.backend.question.controller;

import com.gloyoo.backend.configuration.AuthenticatedUser;
import com.gloyoo.backend.question.dto.QuestionRequestDto;
import com.gloyoo.backend.question.dto.QuestionResponseDto;
import com.gloyoo.backend.question.dto.QuestionStatisticsDto;
import com.gloyoo.backend.question.service.QuestionService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/questions")
public class QuestionController {
    private final QuestionService questionService;

    public QuestionController(QuestionService questionService) {
        this.questionService = questionService;
    }

    @GetMapping
    public List<QuestionResponseDto> getAllQuestions(
            @RequestParam(required = true) UUID surveyId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return questionService.getQuestionsBySurvey(surveyId, user.id());
    }

    @GetMapping("/{id}")
    public QuestionResponseDto getQuestionById(@PathVariable UUID id) {
        return questionService.getQuestionById(id);
    }

    @GetMapping("/{id}/statistics")
    public QuestionStatisticsDto getQuestionStatistics(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return questionService.getStatistics(id, user.id());
    }

    @PostMapping
    public ResponseEntity<QuestionResponseDto> createQuestion(
            @Valid @RequestBody QuestionRequestDto requestDto,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(questionService.createQuestion(requestDto, user.id()));
    }

    @PutMapping("/{id}")
    public QuestionResponseDto updateQuestion(
            @PathVariable UUID id,
            @Valid @RequestBody QuestionRequestDto requestDto,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return questionService.updateQuestion(id, requestDto, user.id());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteQuestion(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        questionService.deleteQuestion(id, user.id());
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleEntityNotFound(EntityNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", exception.getMessage()));
    }
}
