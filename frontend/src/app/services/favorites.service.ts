import { Injectable, signal } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class FavoritesService {
  private readonly storageKey = 'arquixpress-favorites';
  readonly productIds = signal<string[]>(JSON.parse(localStorage.getItem(this.storageKey) || '[]'));

  toggle(productId: string): void {
    const current = this.productIds();
    this.productIds.set(current.includes(productId)
      ? current.filter((id) => id !== productId)
      : [...current, productId]);
    this.persist();
  }

  has(productId: string): boolean {
    return this.productIds().includes(productId);
  }

  private persist(): void {
    localStorage.setItem(this.storageKey, JSON.stringify(this.productIds()));
  }
}
