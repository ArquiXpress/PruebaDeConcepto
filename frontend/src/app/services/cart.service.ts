import { Injectable, signal } from '@angular/core';
import { CartItem } from '../models/cart-item';

@Injectable({ providedIn: 'root' })
export class CartService {
  private readonly storageKey = 'arquixpress-cart';
  readonly items = signal<CartItem[]>(JSON.parse(localStorage.getItem(this.storageKey) || '[]'));

  persist(): void {
    localStorage.setItem(this.storageKey, JSON.stringify(this.items()));
  }

  add(productId: string, quantity = 1): void {
    const next = [...this.items()];
    const existing = next.find((item) => item.productId === productId);
    if (existing) {
      existing.quantity = Math.max(1, existing.quantity + quantity);
    } else {
      next.push({ productId, quantity: Math.max(1, quantity) });
    }
    this.items.set(next);
    this.persist();
  }

  setQuantity(productId: string, quantity: number): void {
    const normalized = Math.max(0, Math.floor(Number(quantity) || 0));
    if (normalized === 0) {
      this.remove(productId);
      return;
    }
    this.items.set(this.items().map((item) =>
      item.productId === productId ? { ...item, quantity: normalized } : item
    ));
    this.persist();
  }

  increment(productId: string): void {
    this.add(productId, 1);
  }

  decrement(productId: string): void {
    const current = this.quantityFor(productId);
    this.setQuantity(productId, current - 1);
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
