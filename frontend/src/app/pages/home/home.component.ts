import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { CatalogService, ProductPage } from '../../services/catalog.service';
import { CartService } from '../../services/cart.service';
import { SessionService } from '../../services/session.service';
import { Product } from '../../models/product';
import { FavoritesService } from '../../services/favorites.service';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './home.component.html',
  styleUrl: './home.component.scss',
})
export class HomeComponent implements OnInit, OnDestroy {
  loading = false;
  error = '';
  cartOpen = false;
  featuredIndex = 0;
  heroSlideIndex = signal(0);
  carouselMotion = signal(false);
  page = signal<ProductPage | null>(null);
  private heroTimer?: number;
  private featuredTimer?: number;
  private readonly productCache = new Map<string, Product>();
  readonly heroSlides = [
    {
      title: 'Herramientas y hogar',
      caption: 'Productos listos para comprar, vender y despachar.',
      image: 'https://images.unsplash.com/photo-1504148455328-c376907d081c?auto=format&fit=crop&w=1200&q=80',
    },
    {
      title: 'Tecnologia y accesorios',
      caption: 'Fotos reales, stock visible y detalle por producto.',
      image: 'https://images.unsplash.com/photo-1517336714731-489689fd1ca8?auto=format&fit=crop&w=1200&q=80',
    },
    {
      title: 'Marketplace para vendedores',
      caption: 'Tiendas verificadas y productos publicados por vendedor.',
      image: 'https://images.unsplash.com/photo-1556742502-ec7c0e9f34b1?auto=format&fit=crop&w=1200&q=80',
    },
  ];

  constructor(
    private readonly catalog: CatalogService,
    public readonly cart: CartService,
    public readonly favorites: FavoritesService,
    public readonly session: SessionService,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    this.loadCatalog();
    this.heroTimer = window.setInterval(() => this.nextHeroSlide(), 4500);
    this.featuredTimer = window.setInterval(() => this.nextFeatured(), 5200);
  }

  ngOnDestroy(): void {
    if (this.heroTimer) {
      window.clearInterval(this.heroTimer);
    }
    if (this.featuredTimer) {
      window.clearInterval(this.featuredTimer);
    }
  }

  loadCatalog(): void {
    this.loading = true;
    this.catalog.search('', '', 0, 100).subscribe({
      next: (page) => {
        this.page.set(page);
        page.content.forEach((product) => this.productCache.set(product.id, product));
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
    this.cartOpen = true;
  }

  toggleFavorite(product: Product, event?: Event): void {
    event?.preventDefault();
    event?.stopPropagation();
    this.favorites.toggle(product.id);
  }

  addToCartFromCard(product: Product, event?: Event): void {
    event?.preventDefault();
    event?.stopPropagation();
    this.addToCart(product);
  }

  nextHeroSlide(): void {
    this.heroSlideIndex.set((this.heroSlideIndex() + 1) % this.heroSlides.length);
  }

  currentHeroSlide() {
    return this.heroSlides[this.heroSlideIndex()];
  }

  featured(): Product[] {
    const items = this.page()?.content ?? [];
    const byCategory = new Map<string, Product[]>();
    items.forEach((product) => {
      const categoryItems = byCategory.get(product.category) ?? [];
      categoryItems.push(product);
      byCategory.set(product.category, categoryItems);
    });

    const varied: Product[] = [];
    const categories = Array.from(byCategory.keys()).sort((left, right) => left.localeCompare(right));
    for (let round = 0; varied.length < Math.min(items.length, 12); round += 1) {
      let added = false;
      for (const category of categories) {
        const product = byCategory.get(category)?.[round];
        if (product && !varied.some((item) => item.id === product.id)) {
          varied.push(product);
          added = true;
        }
      }
      if (!added) {
        break;
      }
    }
    return varied;
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
      this.triggerCarouselMotion();
      this.featuredIndex = (this.featuredIndex + 1) % total;
    }
  }

  previousFeatured(): void {
    const total = this.featured().length;
    if (total) {
      this.triggerCarouselMotion();
      this.featuredIndex = (this.featuredIndex - 1 + total) % total;
    }
  }

  private triggerCarouselMotion(): void {
    this.carouselMotion.set(false);
    window.setTimeout(() => this.carouselMotion.set(true), 0);
    window.setTimeout(() => this.carouselMotion.set(false), 420);
  }

  categories(): string[] {
    const values = this.page()?.content?.map((product) => product.category) ?? [];
    return [...new Set(values)].sort((left, right) => left.localeCompare(right));
  }

  productsByCategory(category: string): Product[] {
    return this.page()?.content?.filter((product) => product.category === category) ?? [];
  }

  toggleCart(): void {
    this.cartOpen = !this.cartOpen;
  }

  checkout(): void {
    this.cartOpen = false;
    this.router.navigateByUrl('/checkout');
  }

  productById(productId: string): Product | undefined {
    return this.productCache.get(productId) ?? this.page()?.content?.find((product) => product.id === productId);
  }

  subtotal(): number {
    return this.cart.items().reduce((sum, item) => {
      const product = this.productById(item.productId);
      return sum + (product?.price ?? 0) * item.quantity;
    }, 0);
  }
}
