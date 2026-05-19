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
  imageUrls: [],
  price: 0,
  stockAvailable: 1,
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
      this.error.set('Completa titulo, descripcion, categoria, al menos una foto, precio mayor a 0 y stock valido.');
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
      error: (error) => {
        this.error.set(error?.error?.message || 'No se pudo guardar el producto. Revisa los datos e intentalo de nuevo.');
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
      imageUrls: product.imageUrls?.length ? product.imageUrls : [product.imageUrl],
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

  onProductImagesSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const files = Array.from(input.files ?? []);
    if (!files.length) {
      return;
    }
    this.error.set('');
    Promise.all(files.map((file) => this.readImage(file)))
      .then((images) => {
        this.form.imageUrls = [...(this.form.imageUrls ?? []), ...images];
        this.form.imageUrl = this.form.imageUrls[0];
        input.value = '';
      })
      .catch((message) => this.error.set(String(message)));
  }

  removeFormImage(index: number): void {
    const images = [...(this.form.imageUrls ?? [])];
    images.splice(index, 1);
    this.form.imageUrls = images;
    this.form.imageUrl = images[0] ?? '';
  }

  productImages(product: Product): string[] {
    return product.imageUrls?.length ? product.imageUrls : [product.imageUrl];
  }

  private isValidForm(): boolean {
    return Boolean(
      this.form.title.trim() &&
      this.form.description.trim() &&
      this.form.category.trim() &&
      ((this.form.imageUrls?.length ?? 0) > 0 || Boolean(this.form.imageUrl?.trim())) &&
      Number(this.form.price) > 0 &&
      Number(this.form.stockAvailable) >= 0
    );
  }

  private normalizePayload(payload: SellerProductPayload): SellerProductPayload {
    const imageUrls = (payload.imageUrls?.length ? payload.imageUrls : [payload.imageUrl || ''])
      .map((image) => image.trim())
      .filter(Boolean);
    return {
      title: payload.title.trim(),
      description: payload.description.trim(),
      category: payload.category.trim().toLowerCase(),
      imageUrl: imageUrls[0],
      imageUrls,
      price: Number(payload.price),
      stockAvailable: Number(payload.stockAvailable),
      status: payload.status,
    };
  }

  private readImage(file: File): Promise<string> {
    if (!['image/png', 'image/jpeg'].includes(file.type)) {
      return Promise.reject('Solo se permiten fotos PNG o JPG para publicaciones.');
    }
    return new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.onload = () => resolve(String(reader.result || ''));
      reader.onerror = () => reject('No se pudo leer la foto seleccionada.');
      reader.readAsDataURL(file);
    });
  }
}
