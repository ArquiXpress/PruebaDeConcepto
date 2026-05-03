import { CommonModule } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { CatalogService } from '../../services/catalog.service';
import { CartService } from '../../services/cart.service';
import { FavoritesService } from '../../services/favorites.service';
import { Product } from '../../models/product';

@Component({
  selector: 'app-product-detail-page',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './product-detail-page.component.html',
  styleUrl: './product-detail-page.component.scss',
})
export class ProductDetailPageComponent implements OnInit {
  product = signal<Product | null>(null);
  selectedImage = signal('');
  loading = false;
  error = '';

  constructor(
    private readonly route: ActivatedRoute,
    private readonly catalog: CatalogService,
    public readonly cart: CartService,
    public readonly favorites: FavoritesService
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) {
      this.error = 'Producto no encontrado.';
      return;
    }
    this.loading = true;
    this.catalog.detail(id).subscribe({
      next: (product) => {
        this.product.set(product);
        this.selectedImage.set(this.productImages(product)[0]);
        this.loading = false;
      },
      error: () => {
        this.error = 'No se pudo cargar el producto.';
        this.loading = false;
      },
    });
  }

  productImages(product: Product): string[] {
    const images = product.imageUrls?.filter(Boolean) ?? [];
    return images.length ? images : [product.imageUrl];
  }

  selectImage(image: string): void {
    this.selectedImage.set(image);
  }
}
