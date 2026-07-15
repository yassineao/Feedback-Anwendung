package com.gloyoo.backend.answer.repository;

import com.gloyoo.backend.answer.entity.Answer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AnswerRepository extends JpaRepository<Answer, UUID> {
    List<Answer> findAllByQuestionId(UUID questionId);

    boolean existsByUserIdAndQuestionSurveyId(UUID userId, UUID surveyId);
}
