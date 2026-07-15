package com.gloyoo.backend.question.service;

import com.gloyoo.backend.answer.entity.Answer;
import com.gloyoo.backend.answer.repository.AnswerRepository;
import com.gloyoo.backend.question.dto.ChoiceStatisticDto;
import com.gloyoo.backend.question.dto.QuestionRequestDto;
import com.gloyoo.backend.question.dto.QuestionResponseDto;
import com.gloyoo.backend.question.dto.QuestionStatisticsDto;
import com.gloyoo.backend.question.entity.Question;
import com.gloyoo.backend.question.entity.QuestionType;
import com.gloyoo.backend.question.repository.QuestionRepository;
import com.gloyoo.backend.survey.entity.Survey;
import com.gloyoo.backend.survey.service.SurveyService;
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
    private final SurveyService surveyService;

    public QuestionService(
            QuestionRepository questionRepository,
            AnswerRepository answerRepository,
            SurveyService surveyService
    ) {
        this.questionRepository = questionRepository;
        this.answerRepository = answerRepository;
        this.surveyService = surveyService;
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

    public List<QuestionResponseDto> getQuestionsBySurvey(UUID surveyId, UUID ownerId) {
        surveyService.findOwnedSurvey(surveyId, ownerId);
        return questionRepository.findAllBySurveyId(surveyId)
                .stream()
                .map(this::toResponseDto)
                .toList();
    }

    public QuestionResponseDto createQuestion(QuestionRequestDto requestDto, UUID ownerId) {
        ChoiceFormat.parseQuestionChoices(requestDto.getQuestion(), requestDto.getType());
        Survey survey = surveyService.findOwnedSurvey(requestDto.getSurveyId(), ownerId);
        Question question = Question.builder()
                .question(requestDto.getQuestion())
                .type(requestDto.getType())
                .survey(survey)
                .build();

        return toResponseDto(questionRepository.save(question));
    }

    public QuestionResponseDto updateQuestion(UUID id, QuestionRequestDto requestDto, UUID ownerId) {
        ChoiceFormat.parseQuestionChoices(requestDto.getQuestion(), requestDto.getType());
        Question question = findQuestionById(id);
        if (!question.getSurvey().getOwnerId().equals(ownerId)) {
            throw new EntityNotFoundException("Question not found with id: " + id);
        }
        Survey survey = surveyService.findOwnedSurvey(requestDto.getSurveyId(), ownerId);
        question.setQuestion(requestDto.getQuestion());
        question.setType(requestDto.getType());
        question.setSurvey(survey);

        return toResponseDto(questionRepository.save(question));
    }

    public void deleteQuestion(UUID id, UUID ownerId) {
        Question question = findQuestionById(id);
        if (!question.getSurvey().getOwnerId().equals(ownerId)) {
            throw new EntityNotFoundException("Question not found with id: " + id);
        }
        questionRepository.delete(question);
    }

    public QuestionStatisticsDto getStatistics(UUID id, UUID ownerId) {
        Question question = findQuestionById(id);
        if (!question.getSurvey().getOwnerId().equals(ownerId)) {
            throw new EntityNotFoundException("Question not found with id: " + id);
        }

        Map<Integer, String> choices = ChoiceFormat.parseQuestionChoices(
                question.getQuestion(),
                question.getType()
        );
        List<Answer> answers = answerRepository.findAllByQuestionId(id);
        if (question.getType() == QuestionType.Freetext) {
            return freetextStatistics(question, answers);
        }

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

    private QuestionStatisticsDto freetextStatistics(Question question, List<Answer> answers) {
        List<ChoiceStatisticDto> statistics = new ArrayList<>();
        int index = 1;

        for (Answer answer : answers) {
            statistics.add(new ChoiceStatisticDto(index++, answer.getAnswer(), 1, 100.0));
        }

        return new QuestionStatisticsDto(
                question.getId(),
                question.getType(),
                answers.size(),
                statistics
        );
    }

    private QuestionResponseDto toResponseDto(Question question) {
        return QuestionResponseDto.builder()
                .id(question.getId())
                .question(question.getQuestion())
                .type(question.getType())
                .surveyId(question.getSurvey().getId())
                .build();
    }

}
