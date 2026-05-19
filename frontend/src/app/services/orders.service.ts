import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

export interface OrderResponse {
  orderId: string;
  status: string;
  shipmentStatus: string;
  total: number;
  shippingCost: number;
  shippingAddress?: string | null;
  shippingCity?: string | null;
  buyerName?: string;
  buyerEmail?: string;
  lines: Array<{
    productId: string;
    title: string;
    imageUrl: string;
    sellerId?: string | null;
    sellerName?: string;
    sellerEmail?: string;
    sellerAddress?: string;
    sellerCity?: string;
    quantity: number;
    unitPrice: number;
  }>;
}

@Injectable({ providedIn: 'root' })
export class OrdersService {
  constructor(private readonly http: HttpClient) {}

  mine(): Observable<OrderResponse[]> {
    return this.http.get<OrderResponse[]>('/api/orders');
  }
}
