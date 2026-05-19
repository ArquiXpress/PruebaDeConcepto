import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import {
  LogisticsCenter,
  LogisticsOperatorContext,
  LogisticsOrder,
  LogisticsService,
  ShipmentStatus,
} from '../../services/logistics.service';
import { SessionService } from '../../services/session.service';

interface ShipmentVisual {
  title: string;
  description: string;
}

@Component({
  selector: 'app-logistics-hub-page',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './logistics-hub-page.component.html',
  styleUrl: './logistics-hub-page.component.scss',
})
export class LogisticsHubPageComponent implements OnInit {
  context = signal<LogisticsOperatorContext | null>(null);
  centerList = signal<LogisticsCenter[]>([]);
  orders = signal<LogisticsOrder[]>([]);
  selectedOrder = signal<LogisticsOrder | null>(null);
  loading = signal(false);
  updating = signal<string | null>(null);
  error = signal('');
  notice = signal('');

  readonly STATES: { key: ShipmentStatus; label: string }[] = [
    { key: 'PREPARATION', label: 'Preparación' },
    { key: 'IN_ROUTE', label: 'En ruta' },
    { key: 'DELIVERED', label: 'Entregado' },
  ];

  readonly visuals: Record<ShipmentStatus, ShipmentVisual> = {
    PREPARATION: {
      title: 'Empacando pedido',
      description: 'Operario alistando el paquete en bodega.',
    },
    IN_ROUTE: {
      title: 'En camino',
      description: 'Vehículo logístico transportando el pedido al cliente.',
    },
    DELIVERED: {
      title: 'Entregado',
      description: 'Paquete entregado y confirmado por el cliente.',
    },
  };

  readonly summary = computed(() => {
    const list = this.orders();
    return {
      total: list.length,
      preparation: list.filter((o) => o.shipmentStatus === 'PREPARATION').length,
      inRoute: list.filter((o) => o.shipmentStatus === 'IN_ROUTE').length,
      delivered: list.filter((o) => o.shipmentStatus === 'DELIVERED').length,
    };
  });

  constructor(
    private readonly logistics: LogisticsService,
    public readonly session: SessionService
  ) {}

  ngOnInit(): void {
    this.reload();
  }

  reload(): void {
    this.loading.set(true);
    this.error.set('');
    this.notice.set('');
    this.logistics.me().subscribe({
      next: (ctx) => this.context.set(ctx),
      error: () => {},
    });
    this.logistics.centers().subscribe({
      next: (list) => this.centerList.set(list),
      error: () => {},
    });
    this.logistics.orders().subscribe({
      next: (list) => {
        this.orders.set(list);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.error.set('No fue posible cargar los pedidos asignados.');
      },
    });
  }

  nextStatus(current: ShipmentStatus): ShipmentStatus | null {
    if (current === 'PREPARATION') return 'IN_ROUTE';
    if (current === 'IN_ROUTE') return 'DELIVERED';
    return null;
  }

  previousStatus(current: ShipmentStatus): ShipmentStatus | null {
    if (current === 'DELIVERED') return 'IN_ROUTE';
    if (current === 'IN_ROUTE') return 'PREPARATION';
    return null;
  }

  advance(order: LogisticsOrder): void {
    const next = this.nextStatus(order.shipmentStatus);
    if (!next) return;
    this.changeShipment(order, next, `Pedido #${order.orderId} actualizado a "${this.visuals[next].title}".`);
  }

  revert(order: LogisticsOrder): void {
    const previous = this.previousStatus(order.shipmentStatus);
    if (!previous) return;
    this.changeShipment(order, previous, `Pedido #${order.orderId} devuelto a "${this.visuals[previous].title}".`);
  }

  private changeShipment(order: LogisticsOrder, status: ShipmentStatus, message: string): void {
    this.updating.set(order.orderId);
    this.logistics.updateShipment(order.orderId, status).subscribe({
      next: (updated) => {
        this.orders.update((list) =>
          list.map((o) => (o.orderId === updated.orderId ? { ...o, ...updated } : o))
        );
        this.updating.set(null);
        this.notice.set(message);
      },
      error: () => {
        this.updating.set(null);
        this.error.set('La transición no fue permitida por el sistema.');
      },
    });
  }

  centerLabel(centerId?: string | null): string {
    if (!centerId) return 'Sin centro';
    const match = this.centerList().find((c) => c.id === centerId);
    return match ? match.displayName : centerId.substring(0, 8);
  }

  shortId(id: string): string {
    return id.substring(0, 8).toUpperCase();
  }

  getStepIndex(status: ShipmentStatus): number {
    return this.STATES.findIndex((s) => s.key === status);
  }

  formatMoney(value: number): string {
    return new Intl.NumberFormat('es-CO', {
      style: 'currency',
      currency: 'COP',
      maximumFractionDigits: 0,
    }).format(value);
  }

  openOrder(order: LogisticsOrder): void {
    this.selectedOrder.set(order);
  }

  closeOrder(): void {
    this.selectedOrder.set(null);
  }
}
