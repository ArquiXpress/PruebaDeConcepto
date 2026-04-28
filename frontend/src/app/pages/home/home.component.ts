import { CommonModule } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { CatalogService, ProductPage } from '../../services/catalog.service';
import { CartService } from '../../services/cart.service';
import { CartUIService } from '../../services/cart-ui.service';
import { RollbackService } from '../../services/rollback.service';
import { SessionService } from '../../services/session.service';
import { Product } from '../../models/product';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './home.component.html',
  styleUrl: './home.component.scss',
})
export class HomeComponent implements OnInit {
  loading = false;
  error = '';
  rollbackLoading = false;
  featuredIndex = 0;
  page = signal<ProductPage | null>(null);

  constructor(
    private readonly catalog: CatalogService,
    public readonly cart: CartService,
    public readonly cartUI: CartUIService,
    public readonly session: SessionService,
    private readonly rollback: RollbackService,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    this.loadCatalog();
  }

  loadCatalog(): void {
    this.loading = true;
    this.catalog.search('', '', 0, 100).subscribe({
      next: (page) => {
        this.page.set(page);
        this.loading = false;
      },
      error: () => {
        this.error = 'No se pudo cargar el catálogo.';
        this.loading = false;
      },
    });
  }

  addToCart(product: Product): void {
    this.cart.add(product.id);
    this.cartUI.open();
  }

  featured(): Product[] {
    return this.page()?.content?.slice(0, 6) ?? [];
  }

  featuredWindow(): Product[] {
    const items = this.featured();
    if (!items.length) {
      return [];
    }
    return [0, 1, 2]
      .map((offset) => items[(this.featuredIndex + offset) % items.length])
      .filter((product): product is Product => Boolean(product));
  }

  nextFeatured(): void {
    const total = this.featured().length;
    if (total) {
      this.featuredIndex = (this.featuredIndex + 1) % total;
    }
  }

  previousFeatured(): void {
    const total = this.featured().length;
    if (total) {
      this.featuredIndex = (this.featuredIndex - 1 + total) % total;
    }
  }

  categories(): string[] {
    const values = this.page()?.content?.map((product) => product.category) ?? [];
    return [...new Set(values)].sort((left, right) => left.localeCompare(right));
  }

  productsByCategory(category: string): Product[] {
    return this.page()?.content?.filter((product) => product.category === category) ?? [];
  }

  performRollback(): void {
    if (!confirm('¿Estás seguro? Esto restaurará la base de datos al estado inicial y perderás todos los cambios.')) {
      return;
    }

    this.rollbackLoading = true;
    this.rollback.performRollback().subscribe({
      next: (response) => {
        this.rollbackLoading = false;
        alert(response.message);
        // Recargar la página
        window.location.reload();
      },
      error: () => {
        this.rollbackLoading = false;
        alert('Error al hacer rollback. Intenta de nuevo.');
      },
    });
  }
}
