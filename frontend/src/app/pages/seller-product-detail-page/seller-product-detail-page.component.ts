import { CommonModule } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Product } from '../../models/product';
import { ProductQuestion, ProductQuestionService } from '../../services/product-question.service';
import { SellerProductService } from '../../services/seller-product.service';

@Component({
  selector: 'app-seller-product-detail-page',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './seller-product-detail-page.component.html',
  styleUrl: './seller-product-detail-page.component.scss',
})
export class SellerProductDetailPageComponent implements OnInit {
  readonly product = signal<Product | null>(null);
  readonly loading = signal(false);
  readonly error = signal('');
  readonly success = signal('');
  readonly questions = signal<ProductQuestion[]>([]);
  readonly questionsLoading = signal(false);
  readonly answeringId = signal<string | null>(null);
  appealReason = '';
  questionError = '';
  answerDrafts: Record<string, string> = {};

  constructor(
    private readonly route: ActivatedRoute,
    private readonly sellerProducts: SellerProductService,
    private readonly questionsService: ProductQuestionService
  ) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) {
      this.error.set('Publicacion no encontrada.');
      return;
    }
    this.loading.set(true);
    this.error.set('');
    this.sellerProducts.detail(id).subscribe({
      next: (product) => {
        this.product.set(product);
        this.loadQuestions(product.id);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('No se pudo cargar el detalle de la publicacion.');
        this.loading.set(false);
      },
    });
  }

  appeal(): void {
    const product = this.product();
    if (!product) {
      return;
    }
    this.sellerProducts.appeal(product.id, this.appealReason).subscribe({
      next: (updated) => {
        this.product.set(updated);
        this.success.set('Apelacion enviada para revision.');
      },
      error: (error) => this.error.set(error?.error?.message || 'No se pudo enviar la apelacion.'),
    });
  }

  loadQuestions(productId: string): void {
    this.questionsLoading.set(true);
    this.questionError = '';
    this.questionsService.listForProduct(productId).subscribe({
      next: (questions) => {
        this.questions.set(questions);
        this.questionsLoading.set(false);
      },
      error: () => {
        this.questionError = 'No se pudieron cargar las preguntas de esta publicacion.';
        this.questionsLoading.set(false);
      },
    });
  }

  answerQuestion(question: ProductQuestion): void {
    const product = this.product();
    const answer = (this.answerDrafts[question.id] || '').trim();
    if (!product || !answer) {
      this.questionError = 'Escribe una respuesta antes de publicarla.';
      return;
    }
    this.answeringId.set(question.id);
    this.questionError = '';
    this.questionsService.answer(question.id, answer).subscribe({
      next: () => {
        delete this.answerDrafts[question.id];
        this.success.set('Respuesta publicada.');
        this.answeringId.set(null);
        this.loadQuestions(product.id);
      },
      error: (error) => {
        this.questionError = error?.error?.message || 'No se pudo publicar la respuesta.';
        this.answeringId.set(null);
      },
    });
  }

  images(product: Product): string[] {
    return product.imageUrls?.length ? product.imageUrls : [product.imageUrl];
  }
}
