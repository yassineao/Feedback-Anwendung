import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { AuthService } from '../../../../core/auth/auth.service';
import { AnswerService } from '../../../../core/services/answer.service';
import { QuestionService } from '../../../../core/services/question.service';
import { SurveyService } from '../../../../core/services/survey.service';
import { QuestionType, Survey, SurveyQuestion } from '../../../../core/models/survey.models';

type WorkspaceMode = 'add' | 'take' | 'surveys';
type SurveyWizardStep = 'name' | 'question';

@Component({
  selector: 'app-main-page',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './main-page.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class MainPageComponent implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly formBuilder = inject(FormBuilder);
  private readonly answerService = inject(AnswerService);
  private readonly questionService = inject(QuestionService);
  private readonly surveyService = inject(SurveyService);

  protected readonly currentUser = this.authService.currentUser;
  protected readonly isAuthenticated = this.authService.isAuthenticated;
  protected readonly surveys = this.surveyService.surveys;
  protected readonly workspaceMode = signal<WorkspaceMode>('take');
  protected readonly selectedSurveyId = signal('');
  protected readonly selectedManagedSurveyId = signal('');
  protected readonly selectedOption = signal('');
  protected readonly selectedOptions = signal<string[]>([]);
  protected readonly textResponse = signal('');
  protected readonly surveyLookupError = signal('');
  protected readonly surveyLoadError = signal('');
  protected readonly answerSubmitError = signal('');
  protected readonly isLoadingSurveys = signal(false);
  protected readonly isSavingSurvey = signal(false);
  protected readonly isSubmittingAnswer = signal(false);
  protected readonly latestCreatedSurveyId = signal('');
  protected readonly surveyWizardStep = signal<SurveyWizardStep>('name');
  protected readonly draftSurveyTitle = signal('');
  protected readonly draftQuestions = signal<SurveyQuestion[]>([]);
  protected readonly draftChoices = signal<string[]>([]);

  protected readonly surveyNameForm = this.formBuilder.nonNullable.group({
    title: ['', [Validators.required, Validators.pattern(/\S/)]]
  });

  protected readonly questionForm = this.formBuilder.nonNullable.group({
    question: ['', [Validators.required, Validators.pattern(/\S/)]],
    type: ['text' as QuestionType, Validators.required],
    choice: ['']
  });

  protected readonly surveyLookupForm = this.formBuilder.nonNullable.group({
    surveyId: ['', [Validators.required, Validators.pattern(/\S/)]]
  });

  protected readonly selectedSurvey = computed(() => {
    return this.surveyService.findById(this.selectedSurveyId());
  });
  protected readonly selectedManagedSurvey = computed(() => {
    return this.surveyService.findById(this.selectedManagedSurveyId());
  });

  ngOnInit(): void {
    if (this.isAuthenticated()) {
      this.loadOwnedSurveys(false);
    }
  }

  protected setWorkspaceMode(mode: WorkspaceMode): void {
    this.workspaceMode.set(mode);
    this.selectedOption.set('');
    this.selectedOptions.set([]);
    this.textResponse.set('');
    this.surveyLookupError.set('');
    this.answerSubmitError.set('');
    this.surveyLoadError.set('');

    if (mode === 'surveys' || mode === 'take') {
      this.loadOwnedSurveys(mode === 'surveys');
    }
  }

  protected startSurveyQuestions(): void {
    this.surveyNameForm.markAllAsTouched();

    if (this.surveyNameForm.invalid) {
      return;
    }

    this.draftSurveyTitle.set(this.surveyNameForm.controls.title.value.trim());
    this.surveyWizardStep.set('question');
  }

  protected addChoice(): void {
    const choice = this.questionForm.controls.choice.value.trim();

    if (!choice) {
      this.questionForm.controls.choice.markAsTouched();
      return;
    }

    this.draftChoices.update((choices) => [...choices, choice]);
    this.questionForm.controls.choice.reset();
  }

  protected removeChoice(choiceIndex: number): void {
    this.draftChoices.update((choices) => choices.filter((_, index) => index !== choiceIndex));
  }

  protected canSaveQuestion(): boolean {
    const question = this.questionForm.controls.question.value.trim();
    const type = this.questionForm.controls.type.value;

    return this.questionService.canCreateQuestion(question, type, this.draftChoices());
  }

  protected addQuestionAndContinue(): void {
    if (!this.saveDraftQuestion()) {
      return;
    }

    this.resetQuestionForm();
  }

  protected finishSurvey(): void {
    const hasUnsavedQuestion = this.questionForm.controls.question.value.trim().length > 0 || this.draftChoices().length > 0;

    if (hasUnsavedQuestion && !this.saveDraftQuestion()) {
      return;
    }

    if (this.draftQuestions().length === 0) {
      this.questionForm.controls.question.markAsTouched();
      return;
    }

    this.isSavingSurvey.set(true);
    this.surveyLoadError.set('');
    this.surveyService.createSurvey(this.draftSurveyTitle(), this.draftQuestions()).subscribe({
      next: (survey) => {
        this.selectedSurveyId.set(survey.id);
        this.latestCreatedSurveyId.set(survey.id);
        this.surveyLookupForm.controls.surveyId.setValue(survey.id);
        this.selectedOption.set('');
        this.resetSurveyWizard();
        this.isSavingSurvey.set(false);
      },
      error: () => {
        this.surveyLoadError.set('Survey could not be saved. Check the backend connection and try again.');
        this.isSavingSurvey.set(false);
      }
    });
  }

  protected findSurveyById(): void {
    this.surveyLookupError.set('');
    this.surveyLookupForm.markAllAsTouched();

    if (this.surveyLookupForm.invalid) {
      return;
    }

    const surveyId = this.surveyLookupForm.controls.surveyId.value.trim();
    const survey = this.surveyService.findById(surveyId);

    if (!survey) {
      this.selectedSurveyId.set('');
      this.surveyLookupError.set('No survey was found with that ID.');
      return;
    }

    this.selectedSurveyId.set(survey.id);
    this.selectedOption.set('');
    this.selectedOptions.set([]);
    this.textResponse.set('');
  }

  protected toggleSelectedOption(option: string, checked: boolean): void {
    this.selectedOptions.update((options) => {
      if (checked) {
        return options.includes(option) ? options : [...options, option];
      }

      return options.filter((selectedOption) => selectedOption !== option);
    });
  }

  protected submitResponse(): void {
    const survey = this.selectedSurvey();
    const question = survey ? this.firstQuestion(survey) : null;
    const option = this.selectedOption();
    const options = this.selectedOptions();
    const textResponse = this.textResponse().trim();

    if (!survey || !question) {
      return;
    }

    if (question.type === 'text' && !textResponse) {
      return;
    }

    if (question.type === 'single' && !option) {
      return;
    }

    if (question.type === 'multiple' && options.length === 0) {
      return;
    }

    const answer = question.type === 'text' ? textResponse : question.type === 'single' ? option : options;
    const responseKeys = this.answerService.responseKeys(question, answer);

    this.isSubmittingAnswer.set(true);
    this.answerSubmitError.set('');
    this.answerService.submitAnswer(question, answer).subscribe({
      next: () => {
        this.surveyService.incrementResponses(survey.id, responseKeys);
        this.selectedOption.set('');
        this.selectedOptions.set([]);
        this.textResponse.set('');
        this.isSubmittingAnswer.set(false);
      },
      error: (error: unknown) => {
        this.answerSubmitError.set(this.answerErrorMessage(error));
        this.isSubmittingAnswer.set(false);
      }
    });
  }

  protected responseTotal(survey: Survey): number {
    return this.surveyService.responseTotal(survey);
  }

  protected responseEntries(survey: Survey): Array<[string, number]> {
    return this.surveyService.responseEntries(survey);
  }

  protected responsePercent(survey: Survey, responseCount: number): number {
    const total = this.responseTotal(survey);
    return total > 0 ? Math.round((responseCount / total) * 100) : 0;
  }

  protected responseBarHeight(survey: Survey, responseCount: number): number {
    const percent = this.responsePercent(survey, responseCount);
    return responseCount > 0 ? Math.max(percent, 8) : 0;
  }

  protected chartColor(index: number): string {
    const colors = ['#67e8f9', '#c4b5fd', '#86efac', '#fda4af', '#fde68a', '#93c5fd'];
    return colors[index % colors.length];
  }

  protected firstQuestion(survey: Survey): SurveyQuestion | null {
    return survey.questions[0] ?? null;
  }

  protected logout(): void {
    this.authService.logout();
  }

  private loadOwnedSurveys(includeStatistics: boolean): void {
    this.isLoadingSurveys.set(true);
    this.surveyLoadError.set('');
    this.surveyService.loadOwnedSurveys(includeStatistics).subscribe({
      next: () => this.isLoadingSurveys.set(false),
      error: () => {
        this.surveyLoadError.set('Surveys could not be loaded from the database.');
        this.isLoadingSurveys.set(false);
      }
    });
  }

  protected selectManagedSurvey(surveyId: string): void {
    this.selectedManagedSurveyId.set(surveyId);
  }

  private saveDraftQuestion(): boolean {
    this.questionForm.controls.question.markAsTouched();

    if (!this.canSaveQuestion()) {
      return false;
    }

    const formValue = this.questionForm.getRawValue();
    const question = this.questionService.createQuestion(formValue.question, formValue.type, this.draftChoices());

    this.draftQuestions.update((questions) => [...questions, question]);
    return true;
  }

  private resetQuestionForm(): void {
    this.questionForm.reset({
      question: '',
      type: 'text',
      choice: ''
    });
    this.draftChoices.set([]);
  }

  private resetSurveyWizard(): void {
    this.surveyNameForm.reset();
    this.resetQuestionForm();
    this.draftSurveyTitle.set('');
    this.draftQuestions.set([]);
    this.surveyWizardStep.set('name');
  }

  private answerErrorMessage(error: unknown): string {
    if (error instanceof HttpErrorResponse && typeof error.error?.message === 'string') {
      return error.error.message;
    }

    return 'Answer could not be submitted. Check the backend connection and try again.';
  }
}
