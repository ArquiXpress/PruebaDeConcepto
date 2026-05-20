import { CommonModule } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { CatalogService } from '../../services/catalog.service';
import { CartService } from '../../services/cart.service';
import { CartUIService } from '../../services/cart-ui.service';
import { FavoritesService } from '../../services/favorites.service';
import { Product } from '../../models/product';
import { ProductQuestion, ProductQuestionService } from '../../services/product-question.service';
import { SessionService } from '../../services/session.service';

@Component({
  selector: 'app-product-detail-page',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './product-detail-page.component.html',
  styleUrl: './product-detail-page.component.scss',
})
export class ProductDetailPageComponent implements OnInit {
  product = signal<Product | null>(null);
  selectedImage = signal('');
  questions = signal<ProductQuestion[]>([]);
  loading = false;
  error = '';
  questionText = '';
  questionLoading = false;
  answeringId = '';
  questionError = '';
  questionMessage = '';
  answerDrafts: Record<string, string> = {};

  constructor(
    private readonly route: ActivatedRoute,
    private readonly catalog: CatalogService,
    private readonly questionService: ProductQuestionService,
    private readonly cartUI: CartUIService,
    public readonly cart: CartService,
    public readonly favorites: FavoritesService,
    public readonly session: SessionService
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
        this.loadQuestions(product.id);
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

  addToCart(productId: string): void {
    this.cart.add(productId);
    this.cartUI.open();
  }

  loadQuestions(productId: string): void {
    this.questionService.listForProduct(productId).subscribe({
      next: (questions) => this.questions.set(questions),
    });
  }

  askQuestion(productId: string): void {
    if (!this.session.isLoggedIn()) {
      this.questionError = 'Inicia sesion para preguntar sobre este producto.';
      return;
    }
    const question = this.questionText.trim();
    if (!question) {
      this.questionError = 'Escribe una pregunta antes de enviarla.';
      return;
    }
    this.questionLoading = true;
    this.questionError = '';
    this.questionMessage = '';
    this.questionService.ask(productId, question).subscribe({
      next: () => {
        this.questionText = '';
        this.questionMessage = 'Pregunta enviada al vendedor.';
        this.questionLoading = false;
        this.loadQuestions(productId);
      },
      error: (error) => {
        this.questionError = error?.error?.message || 'No se pudo enviar la pregunta.';
        this.questionLoading = false;
      },
    });
  }

  canAnswer(item: Product): boolean {
    return this.session.isSeller() && this.session.userId() === item.sellerId;
  }

  answerQuestion(productId: string, question: ProductQuestion): void {
    const answer = (this.answerDrafts[question.id] || '').trim();
    if (!answer) {
      this.questionError = 'Escribe una respuesta antes de publicarla.';
      return;
    }

    this.answeringId = question.id;
    this.questionError = '';
    this.questionMessage = '';
    this.questionService.answer(question.id, answer).subscribe({
      next: () => {
        delete this.answerDrafts[question.id];
        this.questionMessage = 'Respuesta publicada.';
        this.answeringId = '';
        this.loadQuestions(productId);
      },
      error: (error) => {
        this.questionError = error?.error?.message || 'No se pudo publicar la respuesta.';
        this.answeringId = '';
      },
    });
  }
}
