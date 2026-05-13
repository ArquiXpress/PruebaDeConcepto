import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Product, ProductStatus } from '../../models/product';
import { ProductQuestion, ProductQuestionService } from '../../services/product-question.service';
import { SellerProductPayload, SellerProductService } from '../../services/seller-product.service';
import { SessionService } from '../../services/session.service';

const EMPTY_FORM: SellerProductPayload = {
  title: '',
  description: '',
  category: 'tecnologia',
  imageUrl: 'https://images.unsplash.com/photo-1516321318423-f06f85e504b3?auto=format&fit=crop&w=900&q=80',
  price: 0,
  stockAvailable: 0,
  status: 'ACTIVE',
};

@Component({
  selector: 'app-seller-portal',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './seller-portal.component.html',
  styleUrl: './seller-portal.component.scss',
})
export class SellerPortalComponent implements OnInit {
  readonly products = signal<Product[]>([]);
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly questionsLoading = signal(false);
  readonly error = signal('');
  readonly success = signal('');
  readonly questionError = signal('');
  readonly editingId = signal<string | null>(null);
  readonly questions = signal<ProductQuestion[]>([]);
  readonly answeringId = signal<string | null>(null);
  answerDrafts: Record<string, string> = {};

  readonly categories = [
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

  form: SellerProductPayload = { ...EMPTY_FORM };

  readonly activeProducts = computed(() =>
    this.products().filter((product) => product.status !== 'INACTIVE').length
  );

  readonly totalUnits = computed(() =>
    this.products().reduce((total, product) => total + product.stockAvailable, 0)
  );

  readonly inventoryValue = computed(() =>
    this.products().reduce((total, product) => total + product.price * product.stockAvailable, 0)
  );

  readonly lowStockProducts = computed(() =>
    this.products().filter((product) => product.stockAvailable <= 5)
  );

  readonly pendingQuestions = computed(() =>
    this.questions().filter((question) => !question.answer)
  );

  readonly answeredQuestions = computed(() =>
    this.questions().filter((question) => question.answer)
  );

  constructor(
    private readonly sellerProducts: SellerProductService,
    private readonly productQuestions: ProductQuestionService,
    public readonly session: SessionService
  ) {}

  ngOnInit(): void {
    this.loadProducts();
    this.loadQuestions();
  }

  loadProducts(): void {
    this.loading.set(true);
    this.error.set('');

    this.sellerProducts.listMine().subscribe({
      next: (products) => {
        this.products.set(products);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('No se pudieron cargar tus productos. Verifica que hayas iniciado sesión como vendedor.');
        this.loading.set(false);
      },
    });
  }

  loadQuestions(): void {
    this.questionsLoading.set(true);
    this.questionError.set('');

    this.productQuestions.listForSeller().subscribe({
      next: (questions) => {
        this.questions.set(questions);
        this.questionsLoading.set(false);
      },
      error: () => {
        this.questionError.set('No se pudieron cargar las preguntas de tus productos.');
        this.questionsLoading.set(false);
      },
    });
  }

  answerQuestion(question: ProductQuestion): void {
    const answer = (this.answerDrafts[question.id] || '').trim();
    if (!answer) {
      this.questionError.set('Escribe una respuesta antes de publicarla.');
      return;
    }

    this.answeringId.set(question.id);
    this.questionError.set('');
    this.productQuestions.answer(question.id, answer).subscribe({
      next: () => {
        delete this.answerDrafts[question.id];
        this.success.set('Respuesta publicada.');
        this.answeringId.set(null);
        this.loadQuestions();
      },
      error: () => {
        this.questionError.set('No se pudo publicar la respuesta.');
        this.answeringId.set(null);
      },
    });
  }

  save(): void {
    this.error.set('');
    this.success.set('');

    if (!this.isValidForm()) {
      this.error.set('Completa título, descripción, categoría, imagen, precio mayor a 0 y stock válido.');
      return;
    }

    this.saving.set(true);
    const payload = this.normalizePayload(this.form);
    const editingId = this.editingId();
    const request = editingId
      ? this.sellerProducts.update(editingId, payload)
      : this.sellerProducts.create(payload);

    request.subscribe({
      next: () => {
        this.success.set(editingId ? 'Producto actualizado correctamente.' : 'Producto creado correctamente.');
        this.resetForm();
        this.saving.set(false);
        this.loadProducts();
      },
      error: () => {
        this.error.set('No se pudo guardar el producto. Revisa los datos e inténtalo de nuevo.');
        this.saving.set(false);
      },
    });
  }

  edit(product: Product): void {
    this.editingId.set(product.id);
    this.form = {
      title: product.title,
      description: product.description,
      category: product.category,
      imageUrl: product.imageUrl,
      price: product.price,
      stockAvailable: product.stockAvailable,
      status: (product.status ?? 'ACTIVE') as ProductStatus,
    };
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  cancelEdit(): void {
    this.resetForm();
  }

  changeStock(product: Product, stock: string): void {
    const stockAvailable = Number(stock);
    if (!Number.isFinite(stockAvailable) || stockAvailable < 0) {
      this.error.set('El stock debe ser un número mayor o igual a 0.');
      return;
    }

    this.sellerProducts.updateStock(product.id, stockAvailable).subscribe({
      next: () => {
        this.success.set('Stock actualizado.');
        this.loadProducts();
      },
      error: () => this.error.set('No se pudo actualizar el stock.'),
    });
  }

  toggleStatus(product: Product): void {
    const isInactive = product.status === 'INACTIVE';
    const request = isInactive
      ? this.sellerProducts.activate(product.id)
      : this.sellerProducts.deactivate(product.id);

    request.subscribe({
      next: () => {
        this.success.set(isInactive ? 'Producto publicado.' : 'Producto retirado del catálogo.');
        this.loadProducts();
      },
      error: () => this.error.set('No se pudo cambiar el estado del producto.'),
    });
  }

  resetForm(): void {
    this.editingId.set(null);
    this.form = { ...EMPTY_FORM };
  }

  trackById(_: number, product: Product): string {
    return product.id;
  }

  trackQuestionById(_: number, question: ProductQuestion): string {
    return question.id;
  }

  productTitle(productId: string): string {
    return this.products().find((product) => product.id === productId)?.title || 'Producto';
  }

  private isValidForm(): boolean {
    return Boolean(
      this.form.title.trim() &&
      this.form.description.trim() &&
      this.form.category.trim() &&
      this.form.imageUrl.trim() &&
      Number(this.form.price) > 0 &&
      Number(this.form.stockAvailable) >= 0
    );
  }

  private normalizePayload(payload: SellerProductPayload): SellerProductPayload {
    return {
      title: payload.title.trim(),
      description: payload.description.trim(),
      category: payload.category.trim().toLowerCase(),
      imageUrl: payload.imageUrl.trim(),
      price: Number(payload.price),
      stockAvailable: Number(payload.stockAvailable),
      status: payload.status,
    };
  }
}
