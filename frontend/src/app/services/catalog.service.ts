import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { Product } from '../models/product';

export interface ProductPage {
  content: Product[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

@Injectable({ providedIn: 'root' })
export class CatalogService {
  constructor(private readonly http: HttpClient) {}

  search(query = '', category = '', page = 0, size = 100): Observable<ProductPage> {
    const params = new URLSearchParams();
    if (query.trim()) {
      params.set('query', query.trim());
    }
    if (category.trim()) {
      params.set('category', category.trim());
    }
    params.set('page', String(page));
    params.set('size', String(size));
    return this.http.get<ProductPage>(`/api/products?${params.toString()}`);
  }
}
