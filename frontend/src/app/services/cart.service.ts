import { Injectable, signal } from '@angular/core';
import { CartItem } from '../models/cart-item';

@Injectable({ providedIn: 'root' })
export class CartService {
  private readonly storageKey = 'arquixpress-cart';
  readonly items = signal<CartItem[]>(JSON.parse(localStorage.getItem(this.storageKey) || '[]'));

  persist(): void {
    localStorage.setItem(this.storageKey, JSON.stringify(this.items()));
  }

  add(productId: string): void {
    const next = [...this.items()];
    const existing = next.find((item) => item.productId === productId);
    if (existing) {
      existing.quantity += 1;
    } else {
      next.push({ productId, quantity: 1 });
    }
    this.items.set(next);
    this.persist();
  }

  remove(productId: string): void {
    this.items.set(this.items().filter((item) => item.productId !== productId));
    this.persist();
  }

  clear(): void {
    this.items.set([]);
    this.persist();
  }

  quantityFor(productId: string): number {
    return this.items().find((item) => item.productId === productId)?.quantity ?? 0;
  }

  count(): number {
    return this.items().reduce((sum, item) => sum + item.quantity, 0);
  }
}
