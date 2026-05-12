import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { Product, ProductStatus } from '../models/product';

export interface SellerProductPayload {
  title: string;
  description: string;
  category: string;
  imageUrl: string;
  price: number;
  stockAvailable: number;
  status: ProductStatus;
}

@Injectable({ providedIn: 'root' })
export class SellerProductService {
  private readonly baseUrl = '/api/seller/products';

  constructor(private readonly http: HttpClient) {}

  listMine(): Observable<Product[]> {
    return this.http.get<Product[]>(this.baseUrl);
  }

  create(payload: SellerProductPayload): Observable<Product> {
    return this.http.post<Product>(this.baseUrl, payload);
  }

  update(id: string, payload: SellerProductPayload): Observable<Product> {
    return this.http.put<Product>(`${this.baseUrl}/${id}`, payload);
  }

  updateStock(id: string, stockAvailable: number): Observable<Product> {
    return this.http.patch<Product>(`${this.baseUrl}/${id}/stock`, { stockAvailable });
  }

  activate(id: string): Observable<Product> {
    return this.http.patch<Product>(`${this.baseUrl}/${id}/activate`, {});
  }

  deactivate(id: string): Observable<Product> {
    return this.http.patch<Product>(`${this.baseUrl}/${id}/deactivate`, {});
  }
}