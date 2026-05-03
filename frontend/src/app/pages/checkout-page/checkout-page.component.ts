import { CommonModule } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Product } from '../../models/product';
import { CartService } from '../../services/cart.service';
import { CatalogService } from '../../services/catalog.service';
import { CheckoutResponse, CheckoutService } from '../../services/checkout.service';
import { SessionService } from '../../services/session.service';

const PAYMENT_METHOD_OPTIONS = [
  'Tarjeta de credito',
  'Tarjeta debito',
  'PSE',
  'Transferencia bancaria',
  'Pago contra entrega',
  'Otro',
] as const;

@Component({
  selector: 'app-checkout-page',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './checkout-page.component.html',
  styleUrl: './checkout-page.component.scss',
})
export class CheckoutPageComponent implements OnInit {
  readonly paymentMethodOptions = PAYMENT_METHOD_OPTIONS;
  result = signal<CheckoutResponse | null>(null);
  products: Product[] = [];
  selectedPaymentMethod: (typeof PAYMENT_METHOD_OPTIONS)[number] = PAYMENT_METHOD_OPTIONS[0];
  customPaymentMethod = '';
  errorMessage = '';
  loading = false;

  constructor(
    public readonly cart: CartService,
    private readonly catalog: CatalogService,
    private readonly checkout: CheckoutService,
    public readonly session: SessionService
  ) {}

  ngOnInit(): void {
    this.catalog.search('', '', 0, 100).subscribe({
      next: (page) => {
        this.products = page.content;
      },
    });
  }

  itemCount(): number {
    return this.cart.items().reduce((sum, item) => sum + item.quantity, 0);
  }

  subtotal(): number {
    return this.cart.items().reduce((sum, item) => {
      const product = this.product(item.productId);
      return sum + (product?.price ?? 0) * item.quantity;
    }, 0);
  }

  product(productId: string): Product | undefined {
    return this.products.find((item) => item.id === productId);
  }

  paymentMethod(): string {
    return this.selectedPaymentMethod === 'Otro'
      ? this.customPaymentMethod.trim()
      : this.selectedPaymentMethod;
  }

  isCustomPaymentMethod(): boolean {
    return this.selectedPaymentMethod === 'Otro';
  }

  formatStatus(value: string): string {
    return value
      .toLowerCase()
      .split('_')
      .map((segment) => segment.charAt(0).toUpperCase() + segment.slice(1))
      .join(' ');
  }

  statusClass(value: string): string {
    if (value === 'APPROVED' || value === 'PAID') {
      return 'is-success';
    }
    if (value === 'REJECTED' || value === 'PAYMENT_REJECTED' || value === 'ERROR') {
      return 'is-danger';
    }
    return 'is-warning';
  }

  executeCheckout(): void {
    const items = this.cart.items();
    if (!items.length) {
      return;
    }
    const paymentMethod = this.paymentMethod();
    if (!paymentMethod) {
      this.errorMessage = 'Selecciona o ingresa un metodo de pago para continuar.';
      return;
    }

    this.errorMessage = '';
    this.result.set(null);
    this.loading = true;
    this.checkout.checkout(items, paymentMethod).subscribe({
      next: (response) => {
        this.result.set(response);
        this.cart.clear();
        this.loading = false;
      },
      error: (error) => {
        this.errorMessage = error?.error?.message || 'No se pudo completar el checkout.';
        this.loading = false;
      },
    });
  }
}
