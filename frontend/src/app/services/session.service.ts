import { HttpClient } from '@angular/common/http';
import { Injectable, computed, signal } from '@angular/core';
import { map } from 'rxjs';

export type UserRole = 'CLIENT' | 'SELLER' | 'ADMIN' | 'LOGISTICS' | 'SUPERADMIN';

export interface SessionUser {
  id: string;
  email: string;
  displayName: string;
  roles: UserRole[];
  avatarUrl?: string | null;
  phone?: string | null;
  address?: string | null;
  city?: string | null;
  documentNumber?: string | null;
}

@Injectable({ providedIn: 'root' })
export class SessionService {
  private readonly storageKey = 'arquixpress-session-user';
  readonly currentUser = signal<SessionUser | null>(this.load());
  readonly userId = computed(() => this.currentUser()?.id ?? '');

  constructor(private readonly http: HttpClient) {}

  private load(): SessionUser | null {
    const raw = localStorage.getItem(this.storageKey);
    return raw ? JSON.parse(raw) as SessionUser : null;
  }

  setUser(user: SessionUser): void {
    this.currentUser.set(user);
    localStorage.setItem(this.storageKey, JSON.stringify(user));
  }

  refreshFromBackend(): void {
    const user = this.currentUser();
    if (!user) {
      return;
    }
    this.http.get<SessionUser>(`/api/auth/users/${user.id}`).pipe(
      map((response) => ({
        ...response,
        roles: response.roles as UserRole[],
      }))
    ).subscribe((response) => this.setUser(response));
  }

  clear(): void {
    this.currentUser.set(null);
    localStorage.removeItem(this.storageKey);
  }

  hasRole(role: UserRole): boolean {
    return this.currentUser()?.roles.includes(role) ?? false;
  }

  isLoggedIn(): boolean {
    return this.currentUser() !== null;
  }

  isClient(): boolean {
    return this.hasRole('CLIENT');
  }

  isSeller(): boolean {
    return this.hasRole('SELLER');
  }

  isAdmin(): boolean {
    return this.hasRole('ADMIN');
  }

  isOperator(): boolean {
    return this.hasRole('LOGISTICS');
  }

  canAccessOperations(): boolean {
    return this.hasRole('LOGISTICS') || this.hasRole('ADMIN') || this.hasRole('SUPERADMIN');
  }
}
