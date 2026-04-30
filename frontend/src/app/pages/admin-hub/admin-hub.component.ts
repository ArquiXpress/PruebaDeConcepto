import { Component, signal, effect } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { AdminUIService } from '../../services/admin-ui.service';
import { SessionService } from '../../services/session.service';

interface AdminUser {
  id: string;
  email: string;
  displayName: string;
  roles: string;
}

@Component({
  selector: 'app-admin-hub',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './admin-hub.component.html',
  styleUrl: './admin-hub.component.scss',
})
export class AdminHubComponent {
  users = signal<AdminUser[]>([]);
  usersLoading = false;

  editingUser = signal<AdminUser | null>(null);
  selectedRoles = signal<Set<string>>(new Set());
  editModalOpen = signal(false);
  availableRoles = ['CLIENT', 'SELLER', 'ADMIN', 'LOGISTICS'];

  constructor(
    public readonly adminUI: AdminUIService,
    public readonly session: SessionService,
    private readonly http: HttpClient
  ) {
    effect(() => {
      if (this.adminUI.isOpen() && this.users().length === 0) {
        this.loadUsers();
      }
    });
  }

  loadUsers(): void {
    this.usersLoading = true;
    this.http.get<AdminUser[]>('/api/admin/users').subscribe({
      next: (users) => {
        this.users.set(users);
        this.usersLoading = false;
      },
      error: () => {
        this.usersLoading = false;
      },
    });
  }

  openEditModal(user: AdminUser): void {
    this.editingUser.set(user);
    const currentRoles = new Set(user.roles.split(',').map(r => r.trim()));
    this.selectedRoles.set(currentRoles);
    this.editModalOpen.set(true);
  }

  closeEditModal(): void {
    this.editModalOpen.set(false);
    this.editingUser.set(null);
    this.selectedRoles.set(new Set());
  }

  toggleRole(role: string): void {
    const roles = new Set(this.selectedRoles());
    if (roles.has(role)) {
      roles.delete(role);
    } else {
      roles.add(role);
    }
    this.selectedRoles.set(roles);
  }

  hasRole(role: string): boolean {
    return this.selectedRoles().has(role);
  }

  saveUserRoles(): void {
    const user = this.editingUser();
    if (!user || this.selectedRoles().size === 0) {
      alert('Selecciona al menos un rol');
      return;
    }

    const roles = Array.from(this.selectedRoles());
    this.http.put(`/api/admin/users/${user.id}/roles`, { roles }).subscribe({
      next: () => {
        alert('✓ Roles actualizados correctamente');
        this.closeEditModal();
        this.loadUsers();
      },
      error: () => {
        alert('✗ Error al actualizar los roles');
      },
    });
  }
}
