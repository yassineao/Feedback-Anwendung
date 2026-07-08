package com.gloyoo.backend.question.service;

import com.gloyoo.backend.question.dto.QuestionRequestDto;
import com.gloyoo.backend.question.dto.QuestionResponseDto;
import com.gloyoo.backend.question.entity.Question;
import com.gloyoo.backend.question.repository.QuestionRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class QuestionService {
    private final QuestionRepository questionRepository;

    public QuestionService(QuestionRepository questionRepository) {
        this.questionRepository = questionRepository;
    }

    public List<QuestionResponseDto> getAllQuestions() {
        return questionRepository.findAll()
                .stream()
                .map(this::toResponseDto)
                .toList();
    }

    public QuestionResponseDto getQuestionById(UUID id) {
        return toResponseDto(findQuestionById(id));
    }

    public QuestionResponseDto createQuestion(QuestionRequestDto requestDto) {
        Question question = Question.builder()
                .question(requestDto.getQuestion())
                .type(requestDto.getType())
                .build();

        return toResponseDto(questionRepository.save(question));
    }

    public QuestionResponseDto updateQuestion(UUID id, QuestionRequestDto requestDto) {
        Question question = findQuestionById(id);
        question.setQuestion(requestDto.getQuestion());
        question.setType(requestDto.getType());

        return toResponseDto(questionRepository.save(question));
    }

    public void deleteQuestion(UUID id) {
        Question question = findQuestionById(id);
        questionRepository.delete(question);
    }

    private Question findQuestionById(UUID id) {
        return questionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Question not found with id: " + id));
    }

    private QuestionResponseDto toResponseDto(Question question) {
        return QuestionResponseDto.builder()
                .id(question.getId())
                .question(question.getQuestion())
                .type(question.getType())
                .build();
    }
}
