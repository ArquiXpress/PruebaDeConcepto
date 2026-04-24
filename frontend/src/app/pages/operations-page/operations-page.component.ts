import { CommonModule } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { CatalogService, ProductPage } from '../../services/catalog.service';
import { SessionService } from '../../services/session.service';
import { Product } from '../../models/product';

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

  constructor(
    private readonly catalog: CatalogService,
    private readonly auth: AuthService,
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
}
