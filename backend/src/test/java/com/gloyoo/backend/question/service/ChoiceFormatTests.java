package com.gloyoo.backend.question.service;

import com.gloyoo.backend.question.entity.QuestionType;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChoiceFormatTests {
    @Test
    void parsesNumberedQuestionChoices() {
        Map<Integer, String> choices = ChoiceFormat.parseQuestionChoices(
                "How was it?**[1]**Good**[2]**Bad",
                QuestionType.Single_Choice
        );

        assertThat(choices).containsExactly(
                Map.entry(1, "Good"),
                Map.entry(2, "Bad")
        );
    }

    @Test
    void parsesMultipleSelections() {
        Set<Integer> selected = ChoiceFormat.parseAnswer(
                "[1][3]",
                QuestionType.Multiple_Choice,
                Map.of(1, "One", 2, "Two", 3, "Three")
        );

        assertThat(selected).containsExactly(1, 3);
    }

    @Test
    void rejectsMultipleSelectionsForSingleChoice() {
        assertThatThrownBy(() -> ChoiceFormat.parseAnswer(
                "[1][2]",
                QuestionType.Single_Choice,
                Map.of(1, "One", 2, "Two")
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsUnknownChoice() {
        assertThatThrownBy(() -> ChoiceFormat.parseAnswer(
                "[3]",
                QuestionType.Single_Choice,
                Map.of(1, "One", 2, "Two")
        )).isInstanceOf(IllegalArgumentException.class);
    }
}
