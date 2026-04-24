import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterOutlet } from '@angular/router';
import { RouterLink } from '@angular/router';
import { Router } from '@angular/router';
import { AuthService } from './services/auth.service';
import { CartService } from './services/cart.service';
import { SessionService } from './services/session.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterOutlet, RouterLink],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss',
})
export class AppComponent {
  searchTerm = '';

  constructor(
    public readonly session: SessionService,
    public readonly cart: CartService,
    private readonly auth: AuthService,
    private readonly router: Router
  ) {
    this.session.refreshFromBackend();
  }

  search(): void {
    const query = this.searchTerm.trim();
    this.router.navigate(['/catalogo'], {
      queryParams: query ? { query } : {},
    });
  }

  logout(): void {
    this.auth.logout();
  }
}
