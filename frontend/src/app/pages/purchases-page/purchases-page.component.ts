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
}
