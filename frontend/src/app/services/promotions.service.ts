import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

export type CouponTargetType = 'ALL_CLIENTS' | 'HIGH_VALUE_BUYERS' | 'CATEGORY_BUYERS';
export type OfferStatus = 'PENDING' | 'ACCEPTED' | 'REJECTED';

export interface CouponResponse {
  id: string;
  code: string;
  title: string;
  description: string;
  discountPercent: number;
  targetType: CouponTargetType;
  targetValue?: string | null;
  createdAt: string;
}

export interface CouponPayload {
  code: string;
  title: string;
  description: string;
  discountPercent: number;
  targetType: CouponTargetType;
  targetValue?: string | null;
}

export interface OfferProduct {
  id: string;
  title: string;
  category: string;
  imageUrl: string;
  price: number;
}

export interface OfferRequestResponse {
  id: string;
  sellerId: string;
  sellerName: string;
  sellerEmail: string;
  title: string;
  message: string;
  discountPercent: number;
  status: OfferStatus;
  startsAt: string;
  endsAt: string;
  createdAt: string;
  decidedAt?: string | null;
  products: OfferProduct[];
}

export interface OfferPayload {
  sellerId: string;
  title: string;
  message: string;
  discountPercent: number;
  startsAt: string;
  endsAt: string;
  productIds: string[];
}

@Injectable({ providedIn: 'root' })
export class PromotionsService {
  private readonly baseUrl = '/api/admin/promotions';

  constructor(private readonly http: HttpClient) {}

  listCoupons(): Observable<CouponResponse[]> {
    return this.http.get<CouponResponse[]>(`${this.baseUrl}/coupons`);
  }

  createCoupon(payload: CouponPayload): Observable<CouponResponse> {
    return this.http.post<CouponResponse>(`${this.baseUrl}/coupons`, payload);
  }

  listOffers(): Observable<OfferRequestResponse[]> {
    return this.http.get<OfferRequestResponse[]>(`${this.baseUrl}/offers`);
  }

  createOffer(payload: OfferPayload): Observable<OfferRequestResponse> {
    return this.http.post<OfferRequestResponse>(`${this.baseUrl}/offers`, payload);
  }

  listSellerOffers(): Observable<OfferRequestResponse[]> {
    return this.http.get<OfferRequestResponse[]>(`${this.baseUrl}/seller/offers`);
  }

  acceptOffer(id: string): Observable<OfferRequestResponse> {
    return this.http.patch<OfferRequestResponse>(`${this.baseUrl}/seller/offers/${id}/accept`, {});
  }

  rejectOffer(id: string): Observable<OfferRequestResponse> {
    return this.http.patch<OfferRequestResponse>(`${this.baseUrl}/seller/offers/${id}/reject`, {});
  }
}
