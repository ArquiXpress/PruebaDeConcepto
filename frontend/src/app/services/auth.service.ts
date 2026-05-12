import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { map } from 'rxjs';
import { SessionService, SessionUser, UserRole } from './session.service';

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  id: string;
  email: string;
  displayName: string;
  roles: string[];
  avatarUrl?: string | null;
  phone?: string | null;
  address?: string | null;
  city?: string | null;
  documentNumber?: string | null;
}

export interface RegisterRequest {
  email: string;
  password: string;
  displayName: string;
  phone?: string;
  address?: string;
  city?: string;
  documentNumber?: string;
  avatarUrl?: string;
  roles?: string;
}

export interface ProfileUpdateRequest {
  email: string;
  displayName: string;
  avatarUrl?: string;
  phone?: string;
  address?: string;
  city?: string;
  documentNumber?: string;
}

export interface PasswordResetResponse {
  accepted: boolean;
  expiresAt: string;
  message: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  constructor(
    private readonly http: HttpClient,
    private readonly session: SessionService
  ) {}

  login(request: LoginRequest) {
    return this.http.post<LoginResponse>('/api/auth/login', request).pipe(
      map((response) => {
        const sessionUser: SessionUser = {
          id: response.id,
          email: response.email,
          displayName: response.displayName,
          roles: response.roles as UserRole[],
          avatarUrl: response.avatarUrl,
          phone: response.phone,
          address: response.address,
          city: response.city,
          documentNumber: response.documentNumber,
        };
        this.session.setUser(sessionUser);
        return response;
      })
    );
  }

  register(request: RegisterRequest) {
    return this.http.post<LoginResponse>('/api/auth/register', request).pipe(
      map((response) => {
        const sessionUser: SessionUser = {
          id: response.id,
          email: response.email,
          displayName: response.displayName,
          roles: response.roles as UserRole[],
          avatarUrl: response.avatarUrl,
          phone: response.phone,
          address: response.address,
          city: response.city,
          documentNumber: response.documentNumber,
        };
        this.session.setUser(sessionUser);
        return response;
      })
    );
  }

  createOperator(request: RegisterRequest) {
    return this.http.post<LoginResponse>('/api/auth/operators', request).pipe(
      map((response) => {
        const sessionUser: SessionUser = {
          id: response.id,
          email: response.email,
          displayName: response.displayName,
          roles: response.roles as UserRole[],
          avatarUrl: response.avatarUrl,
          phone: response.phone,
          address: response.address,
          city: response.city,
          documentNumber: response.documentNumber,
        };
        return response;
      })
    );
  }

  listUsers() {
    return this.http.get<LoginResponse[]>('/api/auth/users');
  }

  logout(): void {
    this.session.clear();
  }

  updateProfile(request: ProfileUpdateRequest) {
    return this.http.put<LoginResponse>('/api/auth/profile', request).pipe(
      map((response) => {
        const sessionUser: SessionUser = {
          id: response.id,
          email: response.email,
          displayName: response.displayName,
          roles: response.roles as UserRole[],
          avatarUrl: response.avatarUrl,
          phone: response.phone,
          address: response.address,
          city: response.city,
          documentNumber: response.documentNumber,
        };
        this.session.setUser(sessionUser);
        return response;
      })
    );
  }

  requestPasswordReset(email: string) {
    return this.http.post<PasswordResetResponse>('/api/auth/password-reset', { email });
  }

  confirmPasswordReset(token: string, newPassword: string) {
    return this.http.post<LoginResponse>('/api/auth/password-reset/confirm', { token, newPassword }).pipe(
      map((response) => {
        const sessionUser: SessionUser = {
          id: response.id,
          email: response.email,
          displayName: response.displayName,
          roles: response.roles as UserRole[],
          avatarUrl: response.avatarUrl,
          phone: response.phone,
          address: response.address,
          city: response.city,
          documentNumber: response.documentNumber,
        };
        this.session.setUser(sessionUser);
        return response;
      })
    );
  }
}
