import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { AuthService } from '../../../../core/auth/auth.service';

type AuthMode = 'login' | 'signup';

@Component({
  selector: 'app-auth-page',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './auth-page.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AuthPageComponent {
  private readonly authService = inject(AuthService);
  private readonly formBuilder = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  protected readonly mode = signal<AuthMode>(
    this.route.snapshot.queryParamMap.get('mode') === 'signup' ? 'signup' : 'login'
  );
  protected readonly authError = signal('');
  protected readonly isSubmitting = signal(false);

  protected readonly loginForm = this.formBuilder.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', Validators.required]
  });

  protected readonly signupForm = this.formBuilder.nonNullable.group({
    name: ['', Validators.required],
    email: ['', [Validators.required, Validators.email]],
    password: [
      '',
      [
        Validators.required,
        Validators.pattern(/^(?=.*[A-Z])(?=.*[a-z])(?=.*[0-9])(?=.*[@$!%*?&])[A-Za-z0-9@$!%*?&]{8,16}$/)
      ]
    ]
  });

  protected setMode(mode: AuthMode): void {
    this.mode.set(mode);
    this.authError.set('');
  }

  protected submitLogin(): void {
    this.authError.set('');
    this.loginForm.markAllAsTouched();
    if (this.loginForm.invalid) {
      return;
    }

    this.isSubmitting.set(true);
    this.authService
      .login(this.loginForm.getRawValue())
      .pipe(finalize(() => this.isSubmitting.set(false)))
      .subscribe({
        next: () => void this.router.navigateByUrl('/'),
        error: () => this.authError.set('Sign in failed. Check your email and password.')
      });
  }

  protected submitSignup(): void {
    this.authError.set('');
    this.signupForm.markAllAsTouched();
    if (this.signupForm.invalid) {
      return;
    }

    this.isSubmitting.set(true);
    this.authService
      .signup(this.signupForm.getRawValue())
      .pipe(finalize(() => this.isSubmitting.set(false)))
      .subscribe({
        next: () => void this.router.navigateByUrl('/'),
        error: () => this.authError.set('Account creation failed. Try a different email address.')
      });
  }
}
