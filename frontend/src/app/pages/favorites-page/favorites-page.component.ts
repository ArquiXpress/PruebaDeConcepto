import { CommonModule } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CatalogService } from '../../services/catalog.service';
import { FavoritesService } from '../../services/favorites.service';
import { CartService } from '../../services/cart.service';
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

  constructor(
    private readonly catalog: CatalogService,
    public readonly favorites: FavoritesService,
    public readonly cart: CartService
  ) {}

  ngOnInit(): void {
    this.catalog.search('', '', 0, 100).subscribe((page) => {
      const ids = new Set(this.favorites.productIds());
      this.products.set(page.content.filter((product) => ids.has(product.id)));
    });
  }

  addToCart(product: Product, event: Event): void {
    event.preventDefault();
    event.stopPropagation();
    this.cart.add(product.id);
  }

  toggleFavorite(product: Product, event: Event): void {
    event.preventDefault();
    event.stopPropagation();
    this.favorites.toggle(product.id);
    this.products.set(this.products().filter((item) => item.id !== product.id));
  }
}
