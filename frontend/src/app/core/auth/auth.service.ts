import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { User } from '../models';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);

  private readonly _user = signal<User | null>(null);
  readonly user = this._user.asReadonly();
  readonly isAuthenticated = computed(() => this._user() !== null);
  readonly isAdmin = computed(() => this._user()?.role === 'ADMIN');

  /** Loads the current user from /api/v1/me (401 when not logged in). */
  loadCurrentUser(): Observable<User> {
    return this.http.get<User>('/api/v1/me').pipe(tap((u) => this._user.set(u)));
  }

  /** Spring Security logout (POST /logout, CSRF-protected), then drop the cached user. */
  logout(): Observable<void> {
    return this.http.post<void>('/logout', {}).pipe(tap(() => this._user.set(null)));
  }

  /** Full-page redirect to the Google OAuth2 authorization endpoint. */
  loginWithGoogle(): void {
    window.location.href = '/oauth2/authorization/google';
  }
}
