import { CommonModule } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CheckoutResponse } from '../../services/checkout.service';

interface PaymentSummary {
  title: string;
  rows: Array<{ label: string; value: string }>;
}

interface StoredCheckoutResult {
  checkoutResult: CheckoutResponse;
  paymentSummary?: PaymentSummary | null;
}

@Component({
  selector: 'app-checkout-result-page',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './checkout-result-page.component.html',
  styleUrl: './checkout-result-page.component.scss',
})
export class CheckoutResultPageComponent implements OnInit {
  readonly result = signal<CheckoutResponse | null>(null);
  readonly paymentSummary = signal<PaymentSummary | null>(null);
  errorMessage = '';

  ngOnInit(): void {
    const raw = sessionStorage.getItem('arquixpress-checkout-result');
    if (!raw) {
      this.errorMessage = 'No encontramos un resultado de checkout reciente.';
      return;
    }

    try {
      const stored = JSON.parse(raw) as StoredCheckoutResult;
      this.result.set(stored.checkoutResult);
      this.paymentSummary.set(stored.paymentSummary ?? null);
    } catch {
      this.errorMessage = 'No pudimos leer el resultado del checkout.';
    }
  }

  formatStatus(value: string): string {
    const normalized = value?.toUpperCase();
    const labels: Record<string, string> = {
      APPROVED: 'Pago aprobado',
      REJECTED: 'Pago rechazado',
      PAID: 'Pago confirmado',
      PENDING_PAYMENT: 'Pago pendiente',
      PAYMENT_REJECTED: 'Pago rechazado',
      PREPARATION: 'Preparando envio',
      IN_ROUTE: 'En camino',
      DELIVERED: 'Entregado',
    };
    return labels[normalized] ?? value;
  }

  statusClass(value: string): string {
    const normalized = value?.toUpperCase();
    if (normalized === 'APPROVED' || normalized === 'PAID' || normalized === 'DELIVERED') {
      return 'is-success';
    }
    if (normalized === 'REJECTED' || normalized === 'PAYMENT_REJECTED' || normalized === 'ERROR') {
      return 'is-danger';
    }
    return 'is-warning';
  }
}
