import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

export interface OrderResponse {
  orderId: string;
  status: string;
  shipmentStatus: string;
  total: number;
  lines: Array<{
    productId: string;
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
