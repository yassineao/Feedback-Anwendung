package com.gloyoo.backend.survey.repository;

import com.gloyoo.backend.survey.entity.Survey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SurveyRepository extends JpaRepository<Survey, UUID> {
    List<Survey> findAllByOwnerIdOrderByTitleAsc(UUID ownerId);
}
