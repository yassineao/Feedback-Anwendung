package com.gloyoo.backend.answer.controller;

import com.gloyoo.backend.answer.dto.AnswerRequestDto;
import com.gloyoo.backend.answer.dto.AnswerResponseDto;
import com.gloyoo.backend.answer.service.AnswerService;
import com.gloyoo.backend.configuration.AuthenticatedUser;
import com.gloyoo.backend.user.client.UserClient;
import com.gloyoo.backend.user.dto.UserResponseDto;
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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/answers")
public class AnswerController {
    private final AnswerService answerService;
    private final UserClient userClient;

    public AnswerController(AnswerService answerService, UserClient userClient) {
        this.answerService = answerService;
        this.userClient = userClient;
    }

    @GetMapping
    public List<AnswerResponseDto> getAllAnswers() {
        return answerService.getAllAnswers();
    }

    @GetMapping("/{id}")
    public AnswerResponseDto getAnswerById(@PathVariable UUID id) {
        return answerService.getAnswerById(id);
    }

    @PostMapping
    public ResponseEntity<AnswerResponseDto> createAnswer(
            @Valid @RequestBody AnswerRequestDto requestDto,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @RequestHeader("Authorization") String authorization
    ) {
        UserResponseDto user = getUser(authenticatedUser, authorization);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(answerService.createAnswer(requestDto, user.getId()));
    }

    @PutMapping("/{id}")
    public AnswerResponseDto updateAnswer(
            @PathVariable UUID id,
            @Valid @RequestBody AnswerRequestDto requestDto,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @RequestHeader("Authorization") String authorization
    ) {
        UserResponseDto user = getUser(authenticatedUser, authorization);
        return answerService.updateAnswer(id, requestDto, user.getId());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAnswer(@PathVariable UUID id) {
        answerService.deleteAnswer(id);
        return ResponseEntity.ok().build();
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleEntityNotFound(EntityNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", exception.getMessage()));
    }

    private UserResponseDto getUser(AuthenticatedUser authenticatedUser, String authorization) {
        if (authenticatedUser == null) {
            throw new IllegalStateException("Authenticated user is missing.");
        }
        UserResponseDto user = userClient.getUserById(authenticatedUser.id(), authorization);
        if (user == null || user.getId() == null) {
            throw new EntityNotFoundException("Authenticated user was not found.");
        }
        return user;
    }
}
