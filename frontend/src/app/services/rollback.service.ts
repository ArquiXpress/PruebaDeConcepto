import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface RollbackResponse {
  success: boolean;
  message: string;
}

@Injectable({ providedIn: 'root' })
export class RollbackService {
  constructor(private readonly http: HttpClient) {}

  performRollback(): Observable<RollbackResponse> {
    return this.http.post<RollbackResponse>('/api/admin/rollback', {});
  }
}
