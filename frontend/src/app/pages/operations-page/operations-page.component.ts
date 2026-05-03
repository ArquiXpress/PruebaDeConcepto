import { CommonModule } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { CatalogService, ProductPage } from '../../services/catalog.service';
import { SessionService } from '../../services/session.service';
import { Product } from '../../models/product';
import { HttpClient } from '@angular/common/http';

interface SellerProductDraft {
  title: string;
  description: string;
  imageUrls: string[];
  price: number;
  stockAvailable: number;
}

@Component({
  selector: 'app-operations-page',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './operations-page.component.html',
  styleUrl: './operations-page.component.scss',
})
export class OperationsPageComponent implements OnInit {
  page = signal<ProductPage | null>(null);
  note = '';
  loading = false;
  operatorDisplayName = '';
  operatorEmail = '';
  operatorPassword = '';
  operatorRoles = 'LOGISTICS';
  operatorLoading = false;
  operatorError = '';
  sellerType = 'NATURAL';
  legalDocumentType = 'CEDULA';
  legalDocumentNumber = '';
  documentFileName = '';
  documentFileContent = '';
  documentFileMimeType = '';
  companyName = '';
  companyDescription = '';
  contactPhone = '';
  sellerCategory = 'tecnologia';
  sellerProducts: SellerProductDraft[] = [
    { title: '', description: '', imageUrls: [''], price: 0, stockAvailable: 1 },
  ];
  sellerLoading = false;
  sellerMessage = '';
  sellerError = '';
  readonly categories = ['tecnologia', 'hogar', 'gaming', 'moda', 'deportes', 'telefonia', 'oficina', 'cocina', 'belleza', 'auto'];

  constructor(
    private readonly catalog: CatalogService,
    private readonly auth: AuthService,
    private readonly http: HttpClient,
    public readonly session: SessionService
  ) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.catalog.search('', '', 0, 50).subscribe({
      next: (page) => {
        this.page.set(page);
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      },
    });
  }

  roleProducts(role: 'SELLER' | 'ADMIN'): Product[] {
    const items = this.page()?.content ?? [];
    return role === 'SELLER' ? items.slice(0, 3) : items;
  }

  createOperator(): void {
    this.operatorLoading = true;
    this.operatorError = '';
    this.auth.createOperator({
      displayName: this.operatorDisplayName,
      email: this.operatorEmail,
      password: this.operatorPassword,
      roles: this.operatorRoles,
    }).subscribe({
      next: () => {
        this.operatorLoading = false;
        this.operatorDisplayName = '';
        this.operatorEmail = '';
        this.operatorPassword = '';
        this.operatorRoles = 'LOGISTICS';
        this.load();
      },
      error: () => {
        this.operatorLoading = false;
        this.operatorError = 'No se pudo crear el operador.';
      },
    });
  }

  onDocumentSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    this.documentFileName = file?.name ?? '';
    this.documentFileMimeType = file?.type ?? '';
    this.documentFileContent = '';
    if (!file) {
      return;
    }
    const reader = new FileReader();
    reader.onload = () => {
      const result = String(reader.result || '');
      this.documentFileContent = result.includes(',') ? result.split(',')[1] : result;
    };
    reader.readAsDataURL(file);
  }

  requiredProductCount(): number {
    return this.sellerType === 'JURIDICA' ? 3 : 1;
  }

  emptyProductDraft(): SellerProductDraft {
    return { title: '', description: '', imageUrls: [''], price: 0, stockAvailable: 1 };
  }

  ensureProductRows(): void {
    const required = this.requiredProductCount();
    while (this.sellerProducts.length < required) {
      this.sellerProducts.push(this.emptyProductDraft());
    }
  }

  addProductDraft(): void {
    this.sellerProducts.push(this.emptyProductDraft());
  }

  removeProductDraft(index: number): void {
    if (this.sellerProducts.length <= this.requiredProductCount()) {
      return;
    }
    this.sellerProducts.splice(index, 1);
  }

  addProductImage(product: SellerProductDraft): void {
    product.imageUrls.push('');
  }

  removeProductImage(product: SellerProductDraft, index: number): void {
    if (product.imageUrls.length === 1) {
      product.imageUrls[0] = '';
      return;
    }
    product.imageUrls.splice(index, 1);
  }

  cleanImages(product: SellerProductDraft): string[] {
    return product.imageUrls.map((image) => image.trim()).filter(Boolean);
  }

  productCover(product: SellerProductDraft): string {
    return this.cleanImages(product)[0] || 'https://images.unsplash.com/photo-1504148455328-c376907d081c?auto=format&fit=crop&w=900&q=80';
  }

  productRequirementText(): string {
    return this.sellerType === 'JURIDICA'
      ? 'Las empresas deben proponer minimo 3 productos para revisar la tienda.'
      : 'Como persona natural necesitas 1 publicacion inicial para revisar tu cuenta.';
  }

  submitSellerApplication(): void {
    this.ensureProductRows();
    this.sellerLoading = true;
    this.sellerMessage = '';
    this.sellerError = '';
    this.http.post('/api/seller-applications', {
      sellerType: this.sellerType,
      legalDocumentType: this.legalDocumentType,
      legalDocumentNumber: this.legalDocumentNumber,
      documentFileName: this.documentFileName,
      documentFileContent: this.documentFileContent,
      documentFileMimeType: this.documentFileMimeType,
      companyName: this.companyName,
      companyDescription: this.companyDescription,
      contactPhone: this.contactPhone,
      category: this.sellerCategory,
      products: this.sellerProducts.map((product) => ({
        ...product,
        imageUrl: this.cleanImages(product)[0] || '',
        imageUrls: this.cleanImages(product),
      })),
    }).subscribe({
      next: () => {
        this.sellerLoading = false;
        this.sellerMessage = 'Solicitud enviada. Revisaremos tu documento antes de publicar la tienda.';
      },
      error: (error) => {
        this.sellerLoading = false;
        this.sellerError = error?.error?.message || 'No se pudo enviar la solicitud.';
      },
    });
  }
}
