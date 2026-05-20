import { Component, effect, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { Product } from '../../models/product';
import { AdminUIService } from '../../services/admin-ui.service';
import { OfferRequestResponse, PromotionsService, CouponResponse, CouponTargetType } from '../../services/promotions.service';
import { SellerProductService } from '../../services/seller-product.service';
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
  documentFileContent?: string;
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
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-hub.component.html',
  styleUrl: './admin-hub.component.scss',
})
export class AdminHubComponent {
  activeTab = signal<'users' | 'sellerApplications' | 'promotions'>('users');
  users = signal<AdminUser[]>([]);
  sellerApplications = signal<SellerApplication[]>([]);
  coupons = signal<CouponResponse[]>([]);
  offers = signal<OfferRequestResponse[]>([]);
  products = signal<Product[]>([]);
  usersLoading = false;
  sellerApplicationsLoading = false;
  promotionsLoading = false;
  reviewingApplicationId = signal<string | null>(null);
  editingUser = signal<AdminUser | null>(null);
  selectedRoles = signal<Set<string>>(new Set());
  editModalOpen = signal(false);
  errorMessage = signal('');
  successMessage = signal('');
  readonly baseRoles = ['CLIENT', 'SELLER', 'LOGISTICS'];
  readonly privilegedRoles = ['ADMIN'];
  couponForm = {
    code: 'TECHMONDAYS',
    title: 'Lunes de tecnologia',
    description: 'Usando el codigo TECHMONDAYS recibiras un 20% de descuento en productos de tecnologia.',
    discountPercent: 20,
    targetType: 'CATEGORY_BUYERS' as CouponTargetType,
    targetValue: 'tecnologia',
  };
  offerForm = {
    sellerId: '',
    title: 'Oferta destacada',
    message: 'Queremos incluir estos productos en una oferta temporal del marketplace.',
    discountPercent: 15,
    startsAt: this.localDateTimeOffset(1),
    endsAt: this.localDateTimeOffset(15),
    productIds: new Set<string>(),
  };

  constructor(
    public readonly adminUI: AdminUIService,
    public readonly session: SessionService,
    private readonly http: HttpClient,
    private readonly promotions: PromotionsService,
    private readonly sellerProducts: SellerProductService
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
    this.loadPromotions();
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

  pendingOffers(): number {
    return this.offers().filter((offer) => offer.status === 'PENDING').length;
  }

  loadPromotions(): void {
    this.promotionsLoading = true;
    this.promotions.listCoupons().subscribe({
      next: (coupons) => this.coupons.set(coupons),
      error: () => this.errorMessage.set('No se pudieron cargar los cupones.'),
    });
    this.promotions.listOffers().subscribe({
      next: (offers) => {
        this.offers.set(offers);
        this.promotionsLoading = false;
      },
      error: () => {
        this.errorMessage.set('No se pudieron cargar las ofertas.');
        this.promotionsLoading = false;
      },
    });
    this.sellerProducts.listForOperations().subscribe({
      next: (products) => this.products.set(products),
    });
  }

  createCoupon(): void {
    this.errorMessage.set('');
    this.successMessage.set('');
    this.promotions.createCoupon(this.couponForm).subscribe({
      next: () => {
        this.successMessage.set('Cupon creado y enviado a las notificaciones de los clientes seleccionados.');
        this.loadPromotions();
      },
      error: () => this.errorMessage.set('No se pudo crear el cupon.'),
    });
  }

  createOffer(): void {
    if (!this.offerForm.sellerId || this.offerForm.productIds.size === 0) {
      this.errorMessage.set('Selecciona vendedor y al menos un producto para la oferta.');
      return;
    }
    this.errorMessage.set('');
    this.successMessage.set('');
    this.promotions.createOffer({
      sellerId: this.offerForm.sellerId,
      title: this.offerForm.title,
      message: this.offerForm.message,
      discountPercent: this.offerForm.discountPercent,
      startsAt: new Date(this.offerForm.startsAt).toISOString(),
      endsAt: new Date(this.offerForm.endsAt).toISOString(),
      productIds: Array.from(this.offerForm.productIds),
    }).subscribe({
      next: () => {
        this.successMessage.set('Solicitud enviada al vendedor.');
        this.offerForm.productIds = new Set<string>();
        this.loadPromotions();
      },
      error: () => this.errorMessage.set('No se pudo enviar la solicitud de oferta.'),
    });
  }

  sellersFromProducts(): Array<{ id: string; name: string }> {
    const map = new Map<string, string>();
    this.products().forEach((product) => map.set(product.sellerId, product.sellerId.slice(0, 8)));
    this.offers().forEach((offer) => map.set(offer.sellerId, offer.sellerName || offer.sellerEmail || offer.sellerId.slice(0, 8)));
    return Array.from(map.entries()).map(([id, name]) => ({ id, name }));
  }

  offerProductsForSeller(): Product[] {
    return this.products().filter((product) => product.sellerId === this.offerForm.sellerId);
  }

  toggleOfferProduct(productId: string): void {
    const next = new Set(this.offerForm.productIds);
    next.has(productId) ? next.delete(productId) : next.add(productId);
    this.offerForm.productIds = next;
  }

  private localDateTimeOffset(days: number): string {
    const date = new Date();
    date.setDate(date.getDate() + days);
    date.setMinutes(0, 0, 0);
    const offset = date.getTimezoneOffset();
    const local = new Date(date.getTime() - offset * 60000);
    return local.toISOString().slice(0, 16);
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

  documentUrl(application: SellerApplication): string {
    if (!application.documentFileContent || !application.documentFileMimeType) {
      return '';
    }
    return `data:${application.documentFileMimeType};base64,${application.documentFileContent}`;
  }
}
