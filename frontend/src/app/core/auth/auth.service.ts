import { HttpClient } from '@angular/common/http';
import { computed, inject, Injectable, signal } from '@angular/core';
import { map, Observable, tap } from 'rxjs';

import { AuthApiResponse, AuthSession, LoginCredentials, SignupPayload } from '../interfaces/auth.interfaces';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = 'http://localhost:8081/user';
  private readonly storageKey = 'feedback-hub-session';
  private readonly session = signal<AuthSession | null>(this.readSession());

  readonly currentUser = computed(() => this.session()?.user ?? null);
  readonly isAuthenticated = computed(() => this.session() !== null);
  readonly accessToken = computed(() => this.session()?.accessToken ?? null);
  readonly authorizationHeader = computed(() => {
    const session = this.session();
    return session ? `${session.tokenType} ${session.accessToken}` : null;
  });

  login(credentials: LoginCredentials): Observable<AuthSession> {
    return this.http
      .post<AuthApiResponse>(`${this.apiUrl}/login`, credentials)
      .pipe(
        map((response) => this.toSession(response)),
        tap((session) => this.setSession(session))
      );
  }

  signup(payload: SignupPayload): Observable<AuthSession> {
    return this.http
      .post<AuthApiResponse>(`${this.apiUrl}/register`, payload)
      .pipe(
        map((response) => this.toSession(response)),
        tap((session) => this.setSession(session))
      );
  }

  logout(): void {
    this.session.set(null);
    localStorage.removeItem(this.storageKey);
  }

  private setSession(session: AuthSession): void {
    this.session.set(session);
    localStorage.setItem(this.storageKey, JSON.stringify(session));
  }

  private readSession(): AuthSession | null {
    const rawSession = localStorage.getItem(this.storageKey);

    if (!rawSession) {
      return null;
    }

    try {
      return JSON.parse(rawSession) as AuthSession;
    } catch {
      localStorage.removeItem(this.storageKey);
      return null;
    }
  }

  private toSession(response: AuthApiResponse): AuthSession {
    return {
      accessToken: response.accessToken,
      refreshToken: response.refreshToken,
      tokenType: response.tokenType,
      user: {
        id: response.id,
        name: response.name,
        email: response.email,
        role: response.role
      }
    };
  }
}
