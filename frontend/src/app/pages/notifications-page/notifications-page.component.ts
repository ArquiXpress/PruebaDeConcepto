import { CommonModule } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AppNotification, NotificationService } from '../../services/notification.service';

@Component({
  selector: 'app-notifications-page',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './notifications-page.component.html',
  styleUrl: './notifications-page.component.scss',
})
export class NotificationsPageComponent implements OnInit {
  readonly notifications = signal<AppNotification[]>([]);
  readonly loading = signal(false);
  readonly error = signal('');

  constructor(private readonly notificationService: NotificationService) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set('');
    this.notificationService.list().subscribe({
      next: (notifications) => {
        this.notifications.set(notifications);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('No se pudieron cargar tus notificaciones.');
        this.loading.set(false);
      },
    });
  }

  markRead(notification: AppNotification): void {
    if (notification.read) {
      return;
    }
    this.notificationService.markRead(notification.id).subscribe({
      next: () => this.load(),
    });
  }

  markAllRead(): void {
    this.notificationService.markAllRead().subscribe({
      next: () => this.load(),
    });
  }
}
