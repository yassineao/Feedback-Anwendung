import { Injectable } from '@angular/core';

import { QuestionType, SurveyQuestion } from '../models/survey.models';

@Injectable({
  providedIn: 'root'
})
export class QuestionService {
  canCreateQuestion(text: string, type: QuestionType, choices: string[]): boolean {
    return this.normalizeText(text).length > 0 && (type === 'text' || this.normalizeChoices(choices).length > 1);
  }

  createQuestion(text: string, type: QuestionType, choices: string[]): SurveyQuestion {
    const questionText = this.normalizeText(text);
    const normalizedChoices = type === 'text' ? [] : this.normalizeChoices(choices);

    if (!questionText) {
      throw new Error('Question name is required.');
    }

    if (type !== 'text' && normalizedChoices.length < 2) {
      throw new Error('Choice questions require at least two choices.');
    }

    return {
      text: questionText,
      type,
      choices: normalizedChoices
    };
  }

  private normalizeText(value: string): string {
    return value.trim();
  }

  private normalizeChoices(choices: string[]): string[] {
    return choices.map((choice) => choice.trim()).filter((choice) => choice.length > 0);
  }
}
