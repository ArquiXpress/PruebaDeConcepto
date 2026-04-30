import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

export type ShipmentStatus = 'PREPARATION' | 'IN_ROUTE' | 'DELIVERED';

export interface LogisticsOrderLine {
  productId: string;
  quantity: number;
  unitPrice: number;
}

export interface LogisticsOrder {
  orderId: string;
  status: string;
  shipmentStatus: ShipmentStatus;
  total: number;
  logisticsCenterId?: string | null;
  logisticsOperatorId?: string | null;
  buyerId?: string;
  lines: LogisticsOrderLine[];
}

export interface LogisticsCenter {
  id: string;
  city: string;
  displayName: string;
}

export interface LogisticsOperatorContext {
  userId: string;
  city?: string;
  operatorId?: string;
  centerId?: string;
  centerCity?: string;
  centerName?: string;
}

@Injectable({ providedIn: 'root' })
export class LogisticsService {
  constructor(private readonly http: HttpClient) {}

  me(): Observable<LogisticsOperatorContext> {
    return this.http.get<LogisticsOperatorContext>('/api/logistics/me');
  }

  centers(): Observable<LogisticsCenter[]> {
    return this.http.get<LogisticsCenter[]>('/api/logistics/centers');
  }

  orders(): Observable<LogisticsOrder[]> {
    return this.http.get<LogisticsOrder[]>('/api/logistics/orders');
  }

  updateShipment(orderId: string, status: ShipmentStatus): Observable<LogisticsOrder> {
    return this.http.patch<LogisticsOrder>(`/api/logistics/orders/${orderId}/shipment`, { status });
  }
}
