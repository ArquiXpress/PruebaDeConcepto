import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

export interface ProductQuestion {
  id: string;
  productId: string;
  buyerId: string;
  sellerId: string;
  question: string;
  answer?: string | null;
  answeredBy?: string | null;
  answeredAt?: string | null;
  createdAt: string;
}

@Injectable({ providedIn: 'root' })
export class ProductQuestionService {
  constructor(private readonly http: HttpClient) {}

  listForProduct(productId: string): Observable<ProductQuestion[]> {
    return this.http.get<ProductQuestion[]>(`/api/products/${productId}/questions`);
  }

  ask(productId: string, question: string): Observable<ProductQuestion> {
    return this.http.post<ProductQuestion>(`/api/products/${productId}/questions`, { question });
  }

  listForSeller(): Observable<ProductQuestion[]> {
    return this.http.get<ProductQuestion[]>('/api/seller/questions');
  }

  answer(questionId: string, answer: string): Observable<ProductQuestion> {
    return this.http.patch<ProductQuestion>(`/api/seller/questions/${questionId}/answer`, { answer });
  }
}
