import { CommonModule } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { CatalogService, ProductPage } from '../../services/catalog.service';
import { SessionService } from '../../services/session.service';
import { Product } from '../../models/product';
import { HttpClient } from '@angular/common/http';
import { SellerProductService } from '../../services/seller-product.service';

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
  appeals = signal<Product[]>([]);
  adminProducts = signal<Product[]>([]);
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
  newProduct: SellerProductDraft = { title: '', description: '', imageUrls: [''], price: 0, stockAvailable: 1 };
  newProductCategory = 'tecnologia';
  productLoading = false;
  productMessage = '';
  productError = '';
  sellerLoading = false;
  sellerMessage = '';
  sellerError = '';
  readonly categories = ['tecnologia', 'hogar', 'gaming', 'moda', 'deportes', 'telefonia', 'oficina', 'cocina', 'belleza', 'auto'];

  constructor(
    private readonly catalog: CatalogService,
    private readonly auth: AuthService,
    private readonly http: HttpClient,
    private readonly sellerProductService: SellerProductService,
    public readonly session: SessionService
  ) {}

  ngOnInit(): void {
    this.load();
    this.loadAppeals();
    this.loadAdminProducts();
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
    if (role === 'SELLER') {
      const currentUserId = this.session.currentUser()?.id;
      return items.filter((product) => product.sellerId === currentUserId);
    }
    return this.adminProducts();
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
    const allowed = ['image/png', 'image/jpeg', 'application/pdf', 'application/msword', 'application/vnd.openxmlformats-officedocument.wordprocessingml.document'];
    if (!allowed.includes(file.type)) {
      this.sellerError = 'El documento debe ser PNG, JPG, PDF o Word.';
      input.value = '';
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
    return 'La aprobacion se hace una sola vez. Despues podras publicar productos desde tu panel de vendedor.';
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
      products: [],
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

  publishProduct(): void {
    this.productLoading = true;
    this.productMessage = '';
    this.productError = '';
    this.http.post<Product>('/api/products', {
      title: this.newProduct.title,
      description: this.newProduct.description,
      category: this.newProductCategory,
      imageUrls: this.cleanImages(this.newProduct),
      price: this.newProduct.price,
      stockAvailable: this.newProduct.stockAvailable,
    }).subscribe({
      next: () => {
        this.productLoading = false;
        this.productMessage = 'Producto publicado correctamente.';
        this.newProduct = this.emptyProductDraft();
        this.load();
      },
      error: (error) => {
        this.productLoading = false;
        this.productError = error?.error?.message || 'No se pudo publicar el producto.';
      },
    });
  }

  loadAppeals(): void {
    if (!this.session.isAdmin() && !this.session.hasRole('SUPERADMIN')) {
      this.appeals.set([]);
      return;
    }
    this.sellerProductService.listPendingAppeals().subscribe({
      next: (appeals) => this.appeals.set(appeals),
      error: () => {
        this.note = 'No se pudieron cargar las apelaciones pendientes.';
      },
    });
  }

  loadAdminProducts(): void {
    if (!this.session.isAdmin() && !this.session.hasRole('SUPERADMIN')) {
      this.adminProducts.set([]);
      return;
    }
    this.sellerProductService.listForOperations().subscribe({
      next: (products) => this.adminProducts.set(products),
      error: () => {
        this.note = 'No se pudo cargar el catalogo operativo.';
      },
    });
  }

  onProductImagesSelected(event: Event, product: SellerProductDraft): void {
    const input = event.target as HTMLInputElement;
    const files = Array.from(input.files ?? []);
    if (!files.length) {
      return;
    }
    Promise.all(files.map((file) => this.readProductImage(file)))
      .then((images) => {
        product.imageUrls = [...this.cleanImages(product), ...images];
        input.value = '';
      })
      .catch((message) => {
        this.productError = String(message);
      });
  }

  removeProductAsAdmin(product: Product): void {
    const reason = window.prompt('Motivo de eliminacion de la publicacion', 'Incumplimiento de politicas de publicacion');
    if (reason === null) {
      return;
    }
    this.sellerProductService.removeByModerator(product.id, reason).subscribe({
      next: () => {
        this.note = 'Publicacion eliminada y vendedor notificado.';
        this.load();
        this.loadAdminProducts();
      },
      error: (error) => {
        this.note = error?.error?.message || 'No se pudo eliminar la publicacion.';
      },
    });
  }

  restoreAppeal(product: Product): void {
    const reason = window.prompt('Nota para el vendedor', 'Apelacion aprobada. La publicacion fue restaurada.');
    if (reason === null) {
      return;
    }
    this.sellerProductService.restoreAppeal(product.id, reason).subscribe({
      next: () => {
        this.note = 'Apelacion aprobada y vendedor notificado.';
        this.load();
        this.loadAppeals();
        this.loadAdminProducts();
      },
      error: (error) => {
        this.note = error?.error?.message || 'No se pudo aprobar la apelacion.';
      },
    });
  }

  rejectAppeal(product: Product): void {
    const reason = window.prompt('Motivo para mantener eliminada la publicacion', 'La publicacion sigue incumpliendo politicas.');
    if (reason === null) {
      return;
    }
    this.sellerProductService.rejectAppeal(product.id, reason).subscribe({
      next: () => {
        this.note = 'Apelacion rechazada y vendedor notificado.';
        this.loadAppeals();
      },
      error: (error) => {
        this.note = error?.error?.message || 'No se pudo rechazar la apelacion.';
      },
    });
  }

  private readProductImage(file: File): Promise<string> {
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
