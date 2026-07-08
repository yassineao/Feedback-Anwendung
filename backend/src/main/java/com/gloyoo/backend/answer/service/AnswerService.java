package com.gloyoo.backend.answer.service;

import com.gloyoo.backend.answer.dto.AnswerRequestDto;
import com.gloyoo.backend.answer.dto.AnswerResponseDto;
import com.gloyoo.backend.answer.entity.Answer;
import com.gloyoo.backend.answer.repository.AnswerRepository;
import com.gloyoo.backend.question.entity.Question;
import com.gloyoo.backend.question.repository.QuestionRepository;
import com.gloyoo.backend.question.service.ChoiceFormat;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class AnswerService {
    private final AnswerRepository answerRepository;
    private final QuestionRepository questionRepository;

    public AnswerService(AnswerRepository answerRepository, QuestionRepository questionRepository) {
        this.answerRepository = answerRepository;
        this.questionRepository = questionRepository;
    }

    public List<AnswerResponseDto> getAllAnswers() {
        return answerRepository.findAll()
                .stream()
                .map(this::toResponseDto)
                .toList();
    }

    public AnswerResponseDto getAnswerById(UUID id) {
        return toResponseDto(findAnswerById(id));
    }

    public AnswerResponseDto createAnswer(AnswerRequestDto requestDto, UUID userId) {
        Question question = findQuestionById(requestDto.getQuestionId());
        validateAnswer(requestDto, question);

        Answer answer = Answer.builder()
                .answer(requestDto.getAnswer())
                .userId(userId)
                .question(question)
                .build();

        return toResponseDto(answerRepository.save(answer));
    }

    public AnswerResponseDto updateAnswer(UUID id, AnswerRequestDto requestDto, UUID userId) {
        Answer answer = findAnswerById(id);
        Question question = findQuestionById(requestDto.getQuestionId());
        validateAnswer(requestDto, question);

        answer.setAnswer(requestDto.getAnswer());
        answer.setUserId(userId);
        answer.setQuestion(question);

        return toResponseDto(answerRepository.save(answer));
    }

    public void deleteAnswer(UUID id) {
        Answer answer = findAnswerById(id);
        answerRepository.delete(answer);
    }

    private Answer findAnswerById(UUID id) {
        return answerRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Answer not found with id: " + id));
    }

    private Question findQuestionById(UUID id) {
        return questionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Question not found with id: " + id));
    }

    private void validateAnswer(AnswerRequestDto requestDto, Question question) {
        ChoiceFormat.parseAnswer(
                requestDto.getAnswer(),
                question.getType(),
                ChoiceFormat.parseQuestionChoices(question.getQuestion(), question.getType())
        );
    }

    private AnswerResponseDto toResponseDto(Answer answer) {
        return AnswerResponseDto.builder()
                .id(answer.getId())
                .answer(answer.getAnswer())
                .userId(answer.getUserId())
                .questionId(answer.getQuestion().getId())
                .build();
    }
}
