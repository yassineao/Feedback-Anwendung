export type QuestionType = 'text' | 'single' | 'multiple';

export interface SurveyQuestion {
  id?: string;
  text: string;
  type: QuestionType;
  choices: string[];
}

export interface Survey {
  id: string;
  title: string;
  questions: SurveyQuestion[];
  responses: Record<string, number>;
}
