import { HttpClient, HttpHeaders } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { AuthService } from '../auth/auth.service';
import { SurveyQuestion } from '../models/survey.models';

export interface AnswerApiResponse {
  id: string;
  answer: string;
  userId: string;
  questionId: string;
}

@Injectable({
  providedIn: 'root'
})
export class AnswerService {
  private readonly authService = inject(AuthService);
  private readonly http = inject(HttpClient);
  private readonly apiUrl = 'http://localhost:8080';

  submitAnswer(question: SurveyQuestion, answer: string | string[]): Observable<AnswerApiResponse> {
    if (!question.id) {
      throw new Error('Question must be saved before it can be answered.');
    }

    return this.http.post<AnswerApiResponse>(
      `${this.apiUrl}/answers`,
      {
        questionId: question.id,
        answer: this.toBackendAnswer(question, answer)
      },
      {
        headers: this.authHeaders()
      }
    );
  }

  responseKeys(question: SurveyQuestion, answer: string | string[]): string[] {
    if (question.type === 'text') {
      return [question.text];
    }

    const choices = Array.isArray(answer) ? answer : [answer];
    return choices.map((choice) => `${question.text}: ${choice}`);
  }

  private toBackendAnswer(question: SurveyQuestion, answer: string | string[]): string {
    if (question.type === 'text') {
      return String(answer).trim();
    }

    const selectedChoices = Array.isArray(answer) ? answer : [answer];
    return selectedChoices.map((choice) => `[${this.choiceNumber(question, choice)}]`).join('');
  }

  private choiceNumber(question: SurveyQuestion, choice: string): number {
    const choiceIndex = question.choices.findIndex((item) => item === choice);

    if (choiceIndex < 0) {
      throw new Error('Selected choice does not belong to this question.');
    }

    return choiceIndex + 1;
  }

  private authHeaders(): HttpHeaders {
    const authorization = this.authService.authorizationHeader();

    if (!authorization) {
      throw new Error('Authentication is required.');
    }

    return new HttpHeaders({
      Authorization: authorization
    });
  }
}
