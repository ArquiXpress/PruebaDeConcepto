import { CommonModule } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CartService } from '../../services/cart.service';
import { CheckoutResponse, CheckoutService } from '../../services/checkout.service';
import { SessionService } from '../../services/session.service';

@Component({
  selector: 'app-checkout-page',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './checkout-page.component.html',
  styleUrl: './checkout-page.component.scss',
})
export class CheckoutPageComponent implements OnInit {
  result = signal<CheckoutResponse | null>(null);
  loading = false;
  guestPromptOpen = false;

  constructor(
    public readonly cart: CartService,
    private readonly checkout: CheckoutService,
    public readonly session: SessionService
  ) {}

  ngOnInit(): void {
    this.guestPromptOpen = !this.session.isLoggedIn();
  }

  total(): number {
    return this.cart.items().reduce((sum, item) => sum + item.quantity, 0);
  }

  executeCheckout(): void {
    if (!this.session.isLoggedIn()) {
      this.guestPromptOpen = true;
      return;
    }
    const items = this.cart.items();
    if (!items.length) {
      return;
    }
    this.loading = true;
    this.checkout.checkout(items, this.session.currentUser()!.id, this.session.currentUser()!.roles).subscribe({
      next: (response) => {
        this.result.set(response);
        this.cart.clear();
        this.loading = false;
      },
      error: (error) => {
        this.result.set({
          orderId: '',
          orderStatus: 'ERROR',
          shipmentStatus: 'PREPARATION',
          paymentStatus: 'REJECTED',
          total: 0,
          message: error?.error?.message || 'No se pudo completar el checkout.',
        });
        this.loading = false;
      },
    });
  }
}
