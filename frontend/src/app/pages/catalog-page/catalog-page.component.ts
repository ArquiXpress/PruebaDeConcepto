import { CommonModule } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { CatalogService, ProductPage } from '../../services/catalog.service';
import { CartService } from '../../services/cart.service';
import { CartUIService } from '../../services/cart-ui.service';
import { FavoritesService } from '../../services/favorites.service';
import { Product } from '../../models/product';

const CATEGORIES = [
  'tecnologia',
  'hogar',
  'gaming',
  'moda',
  'deportes',
  'telefonia',
  'oficina',
  'cocina',
  'belleza',
  'auto',
];

@Component({
  selector: 'app-catalog-page',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './catalog-page.component.html',
  styleUrl: './catalog-page.component.scss',
})
export class CatalogPageComponent implements OnInit {
  query = '';
  category = '';
  loading = false;
  error = '';
  page = signal<ProductPage | null>(null);
  readonly categories = CATEGORIES;
  offersMode = false;

  constructor(
    private readonly catalog: CatalogService,
    private readonly route: ActivatedRoute,
    private readonly cartUI: CartUIService,
    public readonly cart: CartService,
    public readonly favorites: FavoritesService
  ) {}

  ngOnInit(): void {
    this.route.queryParamMap.subscribe((params) => {
      this.query = params.get('query') ?? '';
      this.category = params.get('category') ?? '';
      this.offersMode = params.get('oferta') === 'true';
      this.loadCatalog();
    });
  }

  loadCatalog(): void {
    this.loading = true;
    this.error = '';
    if (this.offersMode) {
      this.catalog.offers().subscribe({
        next: (products) => {
          this.page.set({ content: products, totalElements: products.length, totalPages: 1, size: products.length, number: 0 });
          this.loading = false;
        },
        error: () => {
          this.error = 'No se pudieron cargar las ofertas.';
          this.loading = false;
        },
      });
      return;
    }
    this.catalog.search(this.query, this.category, 0, 200).subscribe({
      next: (page) => {
        this.page.set(page);
        this.loading = false;
      },
      error: () => {
        this.error = 'No se pudo cargar el catalogo.';
        this.loading = false;
      },
    });
  }

  addToCart(product: Product): void {
    this.cart.add(product.id);
    this.cartUI.open();
  }

  toggleFavorite(product: Product): void {
    this.favorites.toggle(product.id);
  }

  trackById(_: number, product: Product): string {
    return product.id;
  }
}
