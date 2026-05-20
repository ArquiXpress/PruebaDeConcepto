import { CommonModule } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { OrdersService, OrderResponse } from '../../services/orders.service';
import { SessionService } from '../../services/session.service';

@Component({
  selector: 'app-purchases-page',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './purchases-page.component.html',
  styleUrl: './purchases-page.component.scss',
})
export class PurchasesPageComponent implements OnInit {
  orders = signal<OrderResponse[]>([]);
  selectedOrder = signal<OrderResponse | null>(null);
  loading = false;
  error = '';

  constructor(
    private readonly ordersService: OrdersService,
    public readonly session: SessionService
  ) {}

  ngOnInit(): void {
    if (!this.session.isLoggedIn()) {
      return;
    }
    this.loading = true;
    this.ordersService.mine().subscribe({
      next: (orders) => {
        this.orders.set(orders);
        this.loading = false;
      },
      error: () => {
        this.error = 'No se pudieron cargar tus compras.';
        this.loading = false;
      },
    });
  }

  openDetail(order: OrderResponse): void {
    this.selectedOrder.set(order);
  }

  closeDetail(): void {
    this.selectedOrder.set(null);
  }

  formatStatus(value: string): string {
    const labels: Record<string, string> = {
      PAID: 'Pago aprobado',
      PENDING_PAYMENT: 'Pago pendiente',
      PAYMENT_REJECTED: 'Pago rechazado',
      PREPARATION: 'Preparando envio',
      IN_ROUTE: 'En camino',
      DELIVERED: 'Entregado',
    };
    return labels[value] || value.toLowerCase().split('_').map((part) => part.charAt(0).toUpperCase() + part.slice(1)).join(' ');
  }

  primaryLine(order: OrderResponse): OrderResponse['lines'][number] | null {
    return order.lines[0] || null;
  }

  extraProductCount(order: OrderResponse): number {
    return Math.max(0, order.lines.length - 1);
  }
}
