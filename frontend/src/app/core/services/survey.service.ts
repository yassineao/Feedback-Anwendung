import { HttpClient, HttpHeaders } from '@angular/common/http';
import { inject, Injectable, signal } from '@angular/core';
import { forkJoin, map, Observable, of, switchMap, tap } from 'rxjs';

import { AuthService } from '../auth/auth.service';
import { BackendQuestionType, QuestionApiResponse, QuestionStatisticsApiResponse, SurveyApiResponse } from '../interfaces/survey-api.interfaces';
import { QuestionType, Survey, SurveyQuestion } from '../models/survey.models';

@Injectable({
  providedIn: 'root'
})
export class SurveyService {
  private readonly authService = inject(AuthService);
  private readonly http = inject(HttpClient);
  private readonly apiUrl = 'http://localhost:8080';

  readonly surveys = signal<Survey[]>([]);

  loadOwnedSurveys(includeStatistics = false): Observable<Survey[]> {
    return this.http
      .get<SurveyApiResponse[]>(`${this.apiUrl}/surveys`, {
        headers: this.authHeaders()
      })
      .pipe(
        switchMap((surveys) => {
          if (surveys.length === 0) {
            return of([]);
          }

          return forkJoin(surveys.map((survey) => this.loadSurveyDetails(survey, includeStatistics)));
        }),
        tap((surveys) => this.surveys.set(surveys))
      );
  }

  createSurvey(title: string, questions: SurveyQuestion[]): Observable<Survey> {
    const surveyTitle = title.trim();

    if (!surveyTitle) {
      throw new Error('Survey name is required.');
    }

    if (questions.length === 0) {
      throw new Error('At least one question is required.');
    }

    return this.http
      .post<SurveyApiResponse>(
        `${this.apiUrl}/surveys`,
        { title: surveyTitle },
        {
          headers: this.authHeaders()
        }
      )
      .pipe(
        switchMap((survey) => {
          return forkJoin(questions.map((question) => this.createQuestion(survey.id, question))).pipe(
            map((createdQuestions) => this.toSurvey(survey, createdQuestions, []))
          );
        }),
        tap((survey) => this.surveys.update((surveys) => [survey, ...surveys]))
      );
  }

  findById(surveyId: string): Survey | null {
    const normalizedSurveyId = surveyId.trim();
    return this.surveys().find((survey) => survey.id === normalizedSurveyId) ?? null;
  }

  incrementResponses(surveyId: string, responseKeys: string[]): void {
    this.surveys.update((surveys) =>
      surveys.map((survey) =>
        survey.id === surveyId
          ? {
              ...survey,
              responses: {
                ...survey.responses,
                ...Object.fromEntries(responseKeys.map((responseKey) => [responseKey, (survey.responses[responseKey] ?? 0) + 1]))
              }
            }
          : survey
      )
    );
  }

  responseTotal(survey: Survey): number {
    return Object.values(survey.responses).reduce((total, count) => total + count, 0);
  }

  responseEntries(survey: Survey): Array<[string, number]> {
    return Object.entries(survey.responses);
  }

  createResponseKey(question: SurveyQuestion, choice?: string): string {
    return question.type === 'text' ? question.text : `${question.text}: ${choice}`;
  }

  private loadSurveyDetails(survey: SurveyApiResponse, includeStatistics: boolean): Observable<Survey> {
    return this.http
      .get<QuestionApiResponse[]>(`${this.apiUrl}/questions`, {
        headers: this.authHeaders(),
        params: {
          surveyId: survey.id
        }
      })
      .pipe(
        switchMap((questions) => {
          if (questions.length === 0) {
            return of(this.toSurvey(survey, [], []));
          }

          if (!includeStatistics) {
            return of(this.toSurvey(survey, questions, []));
          }

          return forkJoin(
            questions.map((question) =>
              this.http.get<QuestionStatisticsApiResponse>(`${this.apiUrl}/questions/${question.id}/statistics`, {
                headers: this.authHeaders()
              })
            )
          ).pipe(map((statistics) => this.toSurvey(survey, questions, statistics)));
        })
      );
  }

  private createQuestion(surveyId: string, question: SurveyQuestion): Observable<QuestionApiResponse> {
    return this.http.post<QuestionApiResponse>(
      `${this.apiUrl}/questions`,
      {
        surveyId,
        question: this.toBackendQuestionText(question),
        type: this.toBackendQuestionType(question.type)
      },
      {
        headers: this.authHeaders()
      }
    );
  }

  private toSurvey(
    survey: SurveyApiResponse,
    questions: QuestionApiResponse[],
    statistics: QuestionStatisticsApiResponse[]
  ): Survey {
    const mappedQuestions = questions.map((question) => this.toSurveyQuestion(question));
    return {
      id: survey.id,
      title: survey.title,
      questions: mappedQuestions,
      responses: this.toResponseBuckets(mappedQuestions, statistics)
    };
  }

  private toSurveyQuestion(question: QuestionApiResponse): SurveyQuestion {
    const type = this.toQuestionType(question.type);
    return {
      id: question.id,
      text: this.toQuestionText(question.question),
      type,
      choices: type === 'text' ? [] : this.toQuestionChoices(question.question)
    };
  }

  private toResponseBuckets(
    questions: SurveyQuestion[],
    statistics: QuestionStatisticsApiResponse[]
  ): Record<string, number> {
    return Object.fromEntries(
      questions.flatMap((question) => {
        const questionStatistics = statistics.find((item) => item.questionId === question.id);

        if (question.type === 'text') {
          if (questionStatistics?.choices.length) {
            return questionStatistics.choices.map((choice, index) => [choice.label || `Answer ${index + 1}`, choice.count]);
          }

          return [[this.createResponseKey(question), questionStatistics?.totalAnswers ?? 0]];
        }

        return question.choices.map((choice) => {
          const choiceStatistics = questionStatistics?.choices.find((item) => item.label === choice);
          return [choice, choiceStatistics?.count ?? 0];
        });
      })
    );
  }

  private toBackendQuestionText(question: SurveyQuestion): string {
    if (question.type === 'text') {
      return question.text;
    }

    return `${question.text}${question.choices.map((choice, index) => `**[${index + 1}]**${choice}`).join('')}`;
  }

  private toQuestionText(value: string): string {
    return value.split('**[1]**')[0].trim();
  }

  private toQuestionChoices(value: string): string[] {
    const choices = [...value.matchAll(/\*\*\[\d+]\*\*([^*]+)/g)];
    return choices.map((choice) => choice[1].trim());
  }

  private toBackendQuestionType(type: QuestionType): BackendQuestionType {
    if (type === 'single') {
      return 'Single_Choice';
    }

    if (type === 'multiple') {
      return 'Multiple_Choice';
    }

    return 'Freetext';
  }

  private toQuestionType(type: BackendQuestionType): QuestionType {
    if (type === 'Single_Choice') {
      return 'single';
    }

    if (type === 'Multiple_Choice') {
      return 'multiple';
    }

    return 'text';
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
