import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { CartItem } from '../models/cart-item';

export interface CheckoutResponse {
  orderId: string;
  orderStatus: string;
  shipmentStatus: string;
  paymentStatus: string;
  total: number;
  message: string;
}

@Injectable({ providedIn: 'root' })
export class CheckoutService {
  constructor(private readonly http: HttpClient) {}

  checkout(items: CartItem[], userId: string, roles: string[]): Observable<CheckoutResponse> {
    return this.http.post<CheckoutResponse>(
      '/api/checkout',
      { items },
      {
        headers: new HttpHeaders({
          'Content-Type': 'application/json',
          'X-User-Id': userId,
          'X-Roles': roles.join(','),
          'Idempotency-Key': `ui-${Date.now()}`,
        }),
      }
    );
  }
}
