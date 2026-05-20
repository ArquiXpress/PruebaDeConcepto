import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { catchError, forkJoin, of } from 'rxjs';
import { CartService } from '../../services/cart.service';
import { CatalogService } from '../../services/catalog.service';
import { Product } from '../../models/product';
import { SessionService } from '../../services/session.service';

@Component({
  selector: 'app-cart-page',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './cart-page.component.html',
  styleUrl: './cart-page.component.scss',
})
export class CartPageComponent implements OnInit {
  products: Product[] = [];
  loading = false;
  error = '';

  constructor(
    public readonly cart: CartService,
    private readonly catalog: CatalogService,
    public readonly session: SessionService
  ) {}

  ngOnInit(): void {
    this.loadCartProducts();
  }

  loadCartProducts(): void {
    const ids = this.cart.items().map((item) => item.productId);
    this.error = '';
    if (!ids.length) {
      this.products = [];
      return;
    }
    this.loading = true;
    forkJoin(ids.map((id) => this.catalog.detail(id).pipe(catchError(() => of(null))))).subscribe({
      next: (products) => {
        this.products = products.filter((product): product is Product => product !== null);
        this.loading = false;
      },
      error: () => {
        this.error = 'No se pudieron cargar todos los productos del carrito.';
        this.loading = false;
      },
    });
  }

  product(productId: string): Product | undefined {
    return this.products.find((item) => item.id === productId);
  }

  total(): number {
    return this.cart.items().reduce((sum, item) => {
      const product = this.product(item.productId);
      return sum + (product?.price ?? 0) * item.quantity;
    }, 0);
  }

  lineSubtotal(productId: string, quantity: number): number {
    return (this.product(productId)?.price ?? 0) * quantity;
  }

  maxQuantity(productId: string): number {
    return this.product(productId)?.stockAvailable ?? 99;
  }

  updateQuantity(productId: string, quantity: number): void {
    const max = this.maxQuantity(productId);
    this.cart.setQuantity(productId, Math.min(Math.max(1, quantity), max));
  }

  increment(productId: string): void {
    const max = this.maxQuantity(productId);
    const next = Math.min(this.cart.quantityFor(productId) + 1, max);
    this.cart.setQuantity(productId, next);
  }
}
