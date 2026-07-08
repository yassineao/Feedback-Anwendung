package com.gloyoo.backend.question.service;

import com.gloyoo.backend.question.entity.QuestionType;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Converts the compact string representation used for choice questions and answers
 * into structured Java collections.
 *
 * Question example:
 *  How was it?**[1]**Good**[2]**Average**[3]**Bad
 *
 * Single-choice answer example:  [1]
 *
 * Multiple-choice answer example:  [1][3]
 */
public final class ChoiceFormat {
    // Finds a numbered choice marker such as **[1]** inside a question.
    private static final Pattern CHOICE_MARKER = Pattern.compile("\\*\\*\\[(\\d+)]\\*\\*");

    // Finds one selected choice such as [1] inside an answer.
    private static final Pattern ANSWER_MARKER = Pattern.compile("\\[(\\d+)]");

    // Utility class: it contains only static operations and must not be instantiated.
    private ChoiceFormat() {
    }

    /**
     * Extracts each numbered choice and its label from the stored question string.
     *
     * @return choices in the same order in which they appear in the question
     * @throws IllegalArgumentException when the format does not match the question type
     */
    public static Map<Integer, String> parseQuestionChoices(String value, QuestionType type) {

        Map<Integer, String> choices = new LinkedHashMap<>();
        Matcher matcher = CHOICE_MARKER.matcher(value);

        int previousChoice = -1;
        int previousLabelStart = -1;

        while (matcher.find()) {
            if (previousChoice < 0 && matcher.start() == 0) {
                throw new IllegalArgumentException("Question text must appear before the first choice.");
            }

            if (previousChoice >= 0) {
                addChoice(choices, previousChoice, value.substring(previousLabelStart, matcher.start()));
            }

            previousChoice = Integer.parseInt(matcher.group(1));
            previousLabelStart = matcher.end();
        }

        if (previousChoice >= 0) {
            addChoice(choices, previousChoice, value.substring(previousLabelStart));
        }

        if (type == QuestionType.Freetext && !choices.isEmpty()) {
            throw new IllegalArgumentException("Freetext questions cannot define choices.");
        }

        if (type != QuestionType.Freetext && choices.size() < 2) {
            throw new IllegalArgumentException(
                    "Choice questions require at least two choices in the format **[1]**First**[2]**Second."
            );
        }
        return choices;
    }

    /**
     * Parses and validates the selected choice numbers in an answer.
     *
     * cannot select a number that was not defined by its question.
     */
    public static Set<Integer> parseAnswer(
            String value,
            QuestionType type,
            Map<Integer, String> choices
    ) {
        if (type == QuestionType.Freetext) {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException("Freetext answers cannot be blank.");
            }
            return Set.of();
        }

        Set<Integer> selected = new LinkedHashSet<>();
        Matcher matcher = ANSWER_MARKER.matcher(value);
        int consumed = 0;

        while (matcher.find()) {
            if (matcher.start() != consumed) {
                throw invalidAnswerFormat(type);
            }

            int choice = Integer.parseInt(matcher.group(1));

            if (!choices.containsKey(choice)) {
                throw new IllegalArgumentException("Unknown choice number: " + choice);
            }

            if (!selected.add(choice)) {
                throw new IllegalArgumentException("Choice " + choice + " cannot be selected more than once.");
            }
            consumed = matcher.end();
        }


        if (selected.isEmpty() || consumed != value.length()) {
            throw invalidAnswerFormat(type);
        }

        if (type == QuestionType.Single_Choice && selected.size() != 1) {
            throw new IllegalArgumentException("Single-choice answers must contain exactly one choice.");
        }
        return selected;
    }


    private static void addChoice(Map<Integer, String> choices, int number, String label) {
        if (number < 1) {
            throw new IllegalArgumentException("Choice numbers must start at 1.");
        }
        if (label.isBlank()) {
            throw new IllegalArgumentException("Choice " + number + " must have a label.");
        }
        if (choices.putIfAbsent(number, label.trim()) != null) {
            throw new IllegalArgumentException("Duplicate choice number: " + number);
        }
    }

    // Creates a type-specific message showing the expected compact answer syntax.
    private static IllegalArgumentException invalidAnswerFormat(QuestionType type) {
        String expected = type == QuestionType.Single_Choice ? "[1]" : "[1][2]";
        return new IllegalArgumentException("Invalid answer format. Expected a value such as " + expected + ".");
    }
}
