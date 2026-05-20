import { CommonModule } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { catchError, forkJoin, of } from 'rxjs';
import { CatalogService } from '../../services/catalog.service';
import { FavoritesService } from '../../services/favorites.service';
import { CartService } from '../../services/cart.service';
import { CartUIService } from '../../services/cart-ui.service';
import { Product } from '../../models/product';

@Component({
  selector: 'app-favorites-page',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './favorites-page.component.html',
  styleUrl: './favorites-page.component.scss',
})
export class FavoritesPageComponent implements OnInit {
  products = signal<Product[]>([]);
  loading = false;
  error = '';

  constructor(
    private readonly catalog: CatalogService,
    private readonly cartUI: CartUIService,
    public readonly favorites: FavoritesService,
    public readonly cart: CartService
  ) {}

  ngOnInit(): void {
    this.loadFavorites();
  }

  loadFavorites(): void {
    const ids = this.favorites.productIds();
    this.error = '';
    if (!ids.length) {
      this.products.set([]);
      return;
    }
    this.loading = true;
    forkJoin(ids.map((id) => this.catalog.detail(id).pipe(catchError(() => of(null))))).subscribe({
      next: (products) => {
        this.products.set(products.filter((product): product is Product => product !== null));
        this.loading = false;
      },
      error: () => {
        this.error = 'No se pudieron cargar tus favoritos.';
        this.loading = false;
      },
    });
  }

  addToCart(product: Product, event: Event): void {
    event.preventDefault();
    event.stopPropagation();
    this.cart.add(product.id);
    this.cartUI.open();
  }

  toggleFavorite(product: Product, event: Event): void {
    event.preventDefault();
    event.stopPropagation();
    this.favorites.toggle(product.id);
    this.products.set(this.products().filter((item) => item.id !== product.id));
  }
}
