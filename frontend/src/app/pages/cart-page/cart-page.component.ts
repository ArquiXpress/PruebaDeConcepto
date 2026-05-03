import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CartService } from '../../services/cart.service';
import { CatalogService } from '../../services/catalog.service';
import { Product } from '../../models/product';
import { SessionService } from '../../services/session.service';

@Component({
  selector: 'app-cart-page',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './cart-page.component.html',
  styleUrl: './cart-page.component.scss',
})
export class CartPageComponent implements OnInit {
  products: Product[] = [];

  constructor(
    public readonly cart: CartService,
    private readonly catalog: CatalogService,
    public readonly session: SessionService
  ) {}

  ngOnInit(): void {
    this.catalog.search('', '', 0, 100).subscribe((page) => {
      this.products = page.content;
    });
  }

  product(productId: string): Product | undefined {
    return this.products.find((item) => item.id === productId);
  }

  total(): number {
    return this.cart.items().reduce((sum, item) => {
      const product = this.product(item.productId);
      return sum + (product?.price ?? 0) * item.quantity;
    }, 0);
  }
}
