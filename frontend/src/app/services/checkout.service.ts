import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { CartItem } from '../models/cart-item';

export interface CheckoutProduct {
  productId: string;
  title: string;
  imageUrl: string;
  quantity: number;
  unitPrice: number;
  subtotal: number;
}

export interface CheckoutResponse {
  orderId: string;
  orderStatus: string;
  shipmentStatus: string;
  paymentStatus: string;
  paymentMethod: string;
  transactionId: string;
  total: number;
  shippingCost: number;
  shippingAddress?: string | null;
  shippingCity?: string | null;
  items: CheckoutProduct[];
  message: string;
}

@Injectable({ providedIn: 'root' })
export class CheckoutService {
  constructor(private readonly http: HttpClient) {}

  checkout(items: CartItem[], paymentMethod: string, shippingAddress: string, shippingCity: string): Observable<CheckoutResponse> {
    return this.http.post<CheckoutResponse>(
      '/api/checkout',
      { items, paymentMethod, shippingAddress, shippingCity },
      {
        headers: new HttpHeaders({
          'Content-Type': 'application/json',
          'Idempotency-Key': `ui-${Date.now()}`,
        }),
      }
    );
  }
}
