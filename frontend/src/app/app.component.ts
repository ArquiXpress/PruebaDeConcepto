import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterOutlet } from '@angular/router';
import { RouterLink } from '@angular/router';
import { Router } from '@angular/router';
import { AuthService } from './services/auth.service';
import { CartService } from './services/cart.service';
import { CartUIService } from './services/cart-ui.service';
import { SessionService } from './services/session.service';
import { CatalogService } from './services/catalog.service';
import { Product } from './models/product';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterOutlet, RouterLink],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss',
})
export class AppComponent {
  searchTerm = '';
  productCache = signal<Map<string, Product>>(new Map());

  constructor(
    public readonly session: SessionService,
    public readonly cart: CartService,
    public readonly cartUI: CartUIService,
    private readonly auth: AuthService,
    private readonly router: Router,
    private readonly catalog: CatalogService
  ) {
    this.session.refreshFromBackend();
    this.loadProductsForCart();
  }

  loadProductsForCart(): void {
    this.catalog.search('', '', 0, 1000).subscribe({
      next: (page) => {
        const cache = new Map<string, Product>();
        page.content.forEach((product) => cache.set(product.id, product));
        this.productCache.set(cache);
      },
    });
  }

  productById(id: string): Product | undefined {
    return this.productCache().get(id);
  }

  subtotal(): number {
    return this.cart.items().reduce((sum, item) => {
      const product = this.productById(item.productId);
      return sum + ((product?.price ?? 0) * item.quantity);
    }, 0);
  }

  checkout(): void {
    this.cartUI.close();
    this.router.navigateByUrl('/checkout');
  }

  search(): void {
    const query = this.searchTerm.trim();
    this.router.navigate(['/catalogo'], {
      queryParams: query ? { query } : {},
    });
  }

  logout(): void {
    this.auth.logout();
  }
}
