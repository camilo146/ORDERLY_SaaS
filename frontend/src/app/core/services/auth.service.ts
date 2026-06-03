import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, firstValueFrom } from 'rxjs';
import { Router } from '@angular/router';
import { AuthResponse, AuthUser, Business, UserRole } from '../models/orderly.models';

/**
 * Authentication service.
 *
 * Security model:
 * - The JWT access token is stored ONLY in memory (private field).
 *   It is never written to localStorage or sessionStorage.
 *   This prevents token theft via XSS — an attacker executing arbitrary JS cannot
 *   read the token from storage APIs.
 *
 * - Non-sensitive session metadata (user profile, business list, active business ID)
 *   is stored in sessionStorage so the UI can render correctly without a token.
 *   sessionStorage is tab-scoped and cleared when the tab/browser closes.
 *
 * - The consequence of memory-only tokens is that a page refresh clears the token
 *   and the user must re-authenticate. This is the correct security trade-off for
 *   a B2B operations dashboard where session continuity is less critical than
 *   preventing credential theft.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http    = inject(HttpClient);
  private readonly router  = inject(Router);

  // Storage keys — only non-sensitive data lands in sessionStorage.
  private readonly USER_KEY      = 'orderly.user';
  private readonly BUSINESS_KEY  = 'orderly.business.id';
  private readonly BUSINESSES_KEY = 'orderly.businesses';

  // Access token lives in memory only. Never persisted.
  private _token: string | null = null;

  private readonly _user$ = new BehaviorSubject<AuthUser | null>(this._loadUser());
  readonly user$ = this._user$.asObservable();

  get currentUser(): AuthUser | null { return this._user$.value; }
  get isAuthenticated(): boolean     { return this._token !== null; }
  get role(): UserRole | null        { return this.currentUser?.role ?? null; }
  get isSuperAdmin(): boolean        { return this.currentUser?.role === 'SUPER_ADMIN'; }
  get isAdmin(): boolean             { return ['ADMIN', 'SUPER_ADMIN'].includes(this.currentUser?.role ?? ''); }
  get isOperator(): boolean          { return ['OPERATOR', 'ADMIN', 'SUPER_ADMIN'].includes(this.currentUser?.role ?? ''); }

  /** Returns the in-memory token. Returns null after a page refresh — user must re-login. */
  getToken(): string | null { return this._token; }

  getActiveBusinessId(): string | null {
    const current   = sessionStorage.getItem(this.BUSINESS_KEY);
    const businesses = this.getBusinesses();
    if (!businesses.length) {
      if (current) sessionStorage.removeItem(this.BUSINESS_KEY);
      return null;
    }
    if (current && businesses.some(b => b.id === current)) {
      return current;
    }
    const fallback = businesses[0].id;
    sessionStorage.setItem(this.BUSINESS_KEY, fallback);
    return fallback;
  }

  getBusinesses(): Business[] {
    const raw = sessionStorage.getItem(this.BUSINESSES_KEY);
    return raw ? JSON.parse(raw) : [];
  }

  setActiveBusinessId(id: string): void {
    sessionStorage.setItem(this.BUSINESS_KEY, id);
  }

  async login(email: string, password: string): Promise<void> {
    const response = await firstValueFrom(
      this.http.post<AuthResponse>('/api/v1/auth/login', { email, password })
    );
    this._saveSession(response);
    this._redirectByRole(response.user.role);
  }

  saveSession(response: AuthResponse): void {
    this._saveSession(response);
  }

  clearSession(): void {
    this._token = null;
    sessionStorage.removeItem(this.USER_KEY);
    sessionStorage.removeItem(this.BUSINESS_KEY);
    sessionStorage.removeItem(this.BUSINESSES_KEY);
    this._user$.next(null);
  }

  logout(): void {
    this.clearSession();
    this.router.navigate(['/login']);
  }

  private _saveSession(response: AuthResponse): void {
    // Token in memory only — never written to any storage API.
    this._token = response.accessToken;

    // Non-sensitive session metadata in sessionStorage (cleared on tab close).
    sessionStorage.setItem(this.USER_KEY, JSON.stringify(response.user));
    if (response.businesses?.length) {
      sessionStorage.setItem(this.BUSINESSES_KEY, JSON.stringify(response.businesses));
      sessionStorage.setItem(this.BUSINESS_KEY, response.businesses[0].id);
    }
    this._user$.next(response.user);
  }

  private _loadUser(): AuthUser | null {
    const raw = sessionStorage.getItem(this.USER_KEY);
    return raw ? JSON.parse(raw) : null;
  }

  private _redirectByRole(role: UserRole): void {
    switch (role) {
      case 'SUPER_ADMIN': this.router.navigate(['/admin']);     break;
      case 'OPERATOR':    this.router.navigate(['/operator']);  break;
      default:            this.router.navigate(['/dashboard']); break;
    }
  }
}
