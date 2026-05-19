import { Component, effect, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterOutlet } from '@angular/router';
import { RouterLink } from '@angular/router';
import { Router } from '@angular/router';
import { AuthService } from './services/auth.service';
import { CartService } from './services/cart.service';
import { CartUIService } from './services/cart-ui.service';
import { AdminUIService } from './services/admin-ui.service';
import { SessionService } from './services/session.service';
import { CatalogService } from './services/catalog.service';
import { NotificationService } from './services/notification.service';
import { AdminHubComponent } from './pages/admin-hub/admin-hub.component';
import { Product } from './models/product';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterOutlet, RouterLink, AdminHubComponent],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss',
})
export class AppComponent {
  searchTerm = '';
  productCache = signal<Map<string, Product>>(new Map());
  guestPromptOpen = signal(false);
  menuOpen = signal(false);
  unreadNotifications = signal(0);
  loadingProductIds = new Set<string>();

  constructor(
    public readonly session: SessionService,
    public readonly cart: CartService,
    public readonly cartUI: CartUIService,
    public readonly adminUI: AdminUIService,
    private readonly auth: AuthService,
    private readonly router: Router,
    private readonly catalog: CatalogService,
    private readonly notifications: NotificationService
  ) {
    this.session.refreshFromBackend();
    this.loadProductsForCart();
    this.loadUnreadNotifications();
    effect(() => {
      this.cart.items().forEach((item) => this.ensureProductLoaded(item.productId));
    });
  }

  loadProductsForCart(): void {
    this.catalog.search('', '', 0, 1000).subscribe({
      next: (page) => {
        const cache = new Map(this.productCache());
        page.content.forEach((product) => cache.set(product.id, product));
        this.productCache.set(cache);
      },
    });
  }

  productById(id: string): Product | undefined {
    return this.productCache().get(id);
  }

  ensureProductLoaded(id: string): void {
    if (this.productCache().has(id) || this.loadingProductIds.has(id)) {
      return;
    }
    this.loadingProductIds.add(id);
    this.catalog.detail(id).subscribe({
      next: (product) => {
        const cache = new Map(this.productCache());
        cache.set(product.id, product);
        this.productCache.set(cache);
        this.loadingProductIds.delete(id);
      },
      error: () => this.loadingProductIds.delete(id),
    });
  }

  subtotal(): number {
    return this.cart.items().reduce((sum, item) => {
      const product = this.productById(item.productId);
      return sum + ((product?.price ?? 0) * item.quantity);
    }, 0);
  }

  checkout(): void {
    if (!this.session.isLoggedIn()) {
      this.showGuestPrompt();
      return;
    }
    this.cartUI.close();
    this.router.navigateByUrl('/checkout');
  }

  showGuestPrompt(): void {
    this.guestPromptOpen.set(true);
  }

  closeGuestPrompt(): void {
    this.guestPromptOpen.set(false);
  }

  goToLogin(): void {
    this.closeGuestPrompt();
    this.router.navigateByUrl('/login');
  }

  openCart(): void {
    this.closeMenu();
    this.cartUI.toggle();
  }

  loadUnreadNotifications(): void {
    if (!this.session.isLoggedIn()) {
      this.unreadNotifications.set(0);
      return;
    }
    this.notifications.unreadCount().subscribe({
      next: (response) => this.unreadNotifications.set(response.count),
      error: () => this.unreadNotifications.set(0),
    });
  }

  toggleMenu(): void {
    this.menuOpen.update((open) => !open);
  }

  closeMenu(): void {
    this.menuOpen.set(false);
  }

  userAddress(): string {
    const address = this.session.currentUser()?.address?.trim();
    return address || 'Agrega tu direccion';
  }

  search(): void {
    const query = this.searchTerm.trim();
    this.closeMenu();
    this.router.navigate(['/catalogo'], {
      queryParams: query ? { query } : {},
    });
  }

  logout(): void {
    this.closeMenu();
    this.auth.logout();
    this.cart.clear();
    this.cartUI.close();
  }
}
