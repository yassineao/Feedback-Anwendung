package com.gloyoo.backend.question.service;

import com.gloyoo.backend.answer.entity.Answer;
import com.gloyoo.backend.answer.repository.AnswerRepository;
import com.gloyoo.backend.question.dto.ChoiceStatisticDto;
import com.gloyoo.backend.question.dto.QuestionRequestDto;
import com.gloyoo.backend.question.dto.QuestionResponseDto;
import com.gloyoo.backend.question.dto.QuestionStatisticsDto;
import com.gloyoo.backend.question.entity.Question;
import com.gloyoo.backend.question.repository.QuestionRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class QuestionService {
    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;

    public QuestionService(QuestionRepository questionRepository, AnswerRepository answerRepository) {
        this.questionRepository = questionRepository;
        this.answerRepository = answerRepository;
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
        ChoiceFormat.parseQuestionChoices(requestDto.getQuestion(), requestDto.getType());
        Question question = Question.builder()
                .question(requestDto.getQuestion())
                .type(requestDto.getType())
                .build();

        return toResponseDto(questionRepository.save(question));
    }

    public QuestionResponseDto updateQuestion(UUID id, QuestionRequestDto requestDto) {
        ChoiceFormat.parseQuestionChoices(requestDto.getQuestion(), requestDto.getType());
        Question question = findQuestionById(id);
        question.setQuestion(requestDto.getQuestion());
        question.setType(requestDto.getType());

        return toResponseDto(questionRepository.save(question));
    }

    public void deleteQuestion(UUID id) {
        Question question = findQuestionById(id);
        questionRepository.delete(question);
    }

    public QuestionStatisticsDto getStatistics(UUID id) {
        Question question = findQuestionById(id);
        Map<Integer, String> choices = ChoiceFormat.parseQuestionChoices(
                question.getQuestion(),
                question.getType()
        );
        List<Answer> answers = answerRepository.findAllByQuestionId(id);
        Map<Integer, Long> counts = new LinkedHashMap<>();
        choices.keySet().forEach(choice -> counts.put(choice, 0L));

        for (Answer answer : answers) {
            Set<Integer> selected = ChoiceFormat.parseAnswer(
                    answer.getAnswer(),
                    question.getType(),
                    choices
            );
            selected.forEach(choice -> counts.computeIfPresent(choice, (key, count) -> count + 1));
        }

        List<ChoiceStatisticDto> statistics = new ArrayList<>();
        choices.forEach((choice, label) -> {
            long count = counts.get(choice);
            double percentage = answers.isEmpty()
                    ? 0
                    : Math.round((count * 10000.0) / answers.size()) / 100.0;
            statistics.add(new ChoiceStatisticDto(choice, label, count, percentage));
        });

        return new QuestionStatisticsDto(
                question.getId(),
                question.getType(),
                answers.size(),
                statistics
        );
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
