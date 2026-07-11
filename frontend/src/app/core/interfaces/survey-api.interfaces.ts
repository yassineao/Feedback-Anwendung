export type BackendQuestionType = 'Single_Choice' | 'Multiple_Choice' | 'Freetext';

export interface SurveyApiResponse {
  id: string;
  title: string;
  ownerId: string;
}

export interface QuestionApiResponse {
  id: string;
  question: string;
  type: BackendQuestionType;
  surveyId: string;
}

export interface QuestionStatisticsApiResponse {
  questionId: string;
  type: BackendQuestionType;
  totalAnswers: number;
  choices: ChoiceStatisticsApiResponse[];
}

export interface ChoiceStatisticsApiResponse {
  choice: number;
  label: string;
  count: number;
  respondentPercentage: number;
}
