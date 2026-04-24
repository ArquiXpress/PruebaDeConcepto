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
}

export interface RegisterRequest {
  email: string;
  password: string;
  displayName: string;
  roles?: string;
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
        };
        return response;
      })
    );
  }

  logout(): void {
    this.session.clear();
  }
}
