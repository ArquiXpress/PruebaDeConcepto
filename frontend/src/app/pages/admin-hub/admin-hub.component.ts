import { Component, effect, signal } from '@angular/core';
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

interface SellerApplicationProduct {
  title: string;
  description: string;
  imageUrl: string;
  imageUrls?: string[];
  price: number;
  stockAvailable: number;
}

interface SellerApplication {
  id: string;
  userId: string;
  applicantName: string;
  applicantEmail: string;
  sellerType: string;
  legalDocumentType: string;
  legalDocumentNumber: string;
  documentFileName?: string;
  documentFileMimeType?: string;
  companyName?: string;
  companyDescription?: string;
  contactPhone?: string;
  category: string;
  products: SellerApplicationProduct[];
  status: string;
  reviewedBy?: string;
  reviewedAt?: string;
  reviewNote?: string;
  approvedProductCount: number;
  createdAt: string;
}

@Component({
  selector: 'app-admin-hub',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './admin-hub.component.html',
  styleUrl: './admin-hub.component.scss',
})
export class AdminHubComponent {
  activeTab = signal<'users' | 'sellerApplications'>('users');
  users = signal<AdminUser[]>([]);
  sellerApplications = signal<SellerApplication[]>([]);
  usersLoading = false;
  sellerApplicationsLoading = false;
  reviewingApplicationId = signal<string | null>(null);
  editingUser = signal<AdminUser | null>(null);
  selectedRoles = signal<Set<string>>(new Set());
  editModalOpen = signal(false);
  errorMessage = signal('');
  successMessage = signal('');
  readonly baseRoles = ['CLIENT', 'SELLER', 'LOGISTICS'];
  readonly privilegedRoles = ['ADMIN'];

  constructor(
    public readonly adminUI: AdminUIService,
    public readonly session: SessionService,
    private readonly http: HttpClient
  ) {
    effect(() => {
      if (this.adminUI.isOpen() && (this.session.isAdmin() || this.session.hasRole('SUPERADMIN'))) {
        queueMicrotask(() => this.loadAdminData());
      }
    });
  }

  loadAdminData(): void {
    this.loadUsers();
    this.loadSellerApplications();
  }

  loadUsers(): void {
    this.usersLoading = true;
    this.errorMessage.set('');
    this.http.get<AdminUser[]>('/api/admin/users').subscribe({
      next: (users) => {
        this.users.set(users);
        this.usersLoading = false;
      },
      error: () => {
        this.usersLoading = false;
        this.errorMessage.set('No se pudo cargar la informacion de usuarios.');
      },
    });
  }

  loadSellerApplications(): void {
    this.sellerApplicationsLoading = true;
    this.errorMessage.set('');
    this.http.get<SellerApplication[]>('/api/admin/seller-applications').subscribe({
      next: (applications) => {
        this.sellerApplications.set(applications);
        this.sellerApplicationsLoading = false;
      },
      error: () => {
        this.sellerApplicationsLoading = false;
        this.errorMessage.set('No se pudieron cargar las solicitudes de vendedores.');
      },
    });
  }

  pendingSellerApplications(): number {
    return this.sellerApplications().filter((application) => application.status === 'PENDING_REVIEW').length;
  }

  openEditModal(user: AdminUser): void {
    if (!this.canEditUser(user)) {
      return;
    }
    this.editingUser.set(user);
    this.selectedRoles.set(new Set(this.userRoles(user)));
    this.errorMessage.set('');
    this.successMessage.set('');
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

  userRoles(user: AdminUser): string[] {
    return user.roles.split(',').map((role) => role.trim()).filter(Boolean);
  }

  isSuperAdminUser(user: AdminUser): boolean {
    return this.userRoles(user).includes('SUPERADMIN');
  }

  isAdminUser(user: AdminUser): boolean {
    return this.userRoles(user).includes('ADMIN');
  }

  availableRoles(): string[] {
    return this.session.hasRole('SUPERADMIN')
      ? [...this.baseRoles, ...this.privilegedRoles]
      : this.baseRoles;
  }

  canEditUser(user: AdminUser): boolean {
    if (this.isSuperAdminUser(user)) {
      return false;
    }
    if (this.session.hasRole('SUPERADMIN')) {
      return true;
    }
    return !this.isAdminUser(user);
  }

  saveUserRoles(): void {
    const user = this.editingUser();
    if (!user || this.selectedRoles().size === 0) {
      this.errorMessage.set('Selecciona al menos un rol.');
      return;
    }

    const roles = Array.from(this.selectedRoles());
    this.errorMessage.set('');
    this.successMessage.set('');
    this.http.put(`/api/admin/users/${user.id}/roles`, { roles }).subscribe({
      next: () => {
        this.successMessage.set('Roles actualizados correctamente.');
        this.closeEditModal();
        this.loadUsers();
      },
      error: () => {
        this.errorMessage.set('No se pudieron actualizar los roles.');
      },
    });
  }

  approveSellerApplication(application: SellerApplication): void {
    if (application.status !== 'PENDING_REVIEW') {
      return;
    }
    this.reviewingApplicationId.set(application.id);
    this.errorMessage.set('');
    this.successMessage.set('');
    this.http.post(`/api/admin/seller-applications/${application.id}/approve`, { note: 'Solicitud aprobada' }).subscribe({
      next: () => {
        this.successMessage.set('Solicitud aprobada. El usuario ya puede vender y sus productos fueron publicados.');
        this.reviewingApplicationId.set(null);
        this.loadAdminData();
      },
      error: () => {
        this.errorMessage.set('No se pudo aprobar la solicitud.');
        this.reviewingApplicationId.set(null);
      },
    });
  }

  rejectSellerApplication(application: SellerApplication): void {
    if (application.status !== 'PENDING_REVIEW') {
      return;
    }
    const note = window.prompt('Motivo del rechazo', 'Documento o informacion incompleta');
    if (note === null) {
      return;
    }
    this.reviewingApplicationId.set(application.id);
    this.errorMessage.set('');
    this.successMessage.set('');
    this.http.post(`/api/admin/seller-applications/${application.id}/reject`, { note }).subscribe({
      next: () => {
        this.successMessage.set('Solicitud rechazada.');
        this.reviewingApplicationId.set(null);
        this.loadSellerApplications();
      },
      error: () => {
        this.errorMessage.set('No se pudo rechazar la solicitud.');
        this.reviewingApplicationId.set(null);
      },
    });
  }

  statusLabel(status: string): string {
    const labels: Record<string, string> = {
      PENDING_REVIEW: 'Pendiente',
      APPROVED: 'Aprobada',
      REJECTED: 'Rechazada',
    };
    return labels[status] ?? status;
  }
}
