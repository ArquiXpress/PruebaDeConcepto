import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { CITY_OPTIONS } from '../../shared/city-options';

@Component({
  selector: 'app-register-page',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './register-page.component.html',
  styleUrl: './register-page.component.scss',
})
export class RegisterPageComponent {
  readonly cityOptions = CITY_OPTIONS;
  displayName = '';
  email = '';
  password = '';
  phone = '';
  address = '';
  city = '';
  documentNumber = '';
  avatarUrl = '';
  loading = false;
  error = '';

  constructor(
    private readonly auth: AuthService,
    private readonly router: Router
  ) {}

  submit(): void {
    this.loading = true;
    this.error = '';
    this.auth.register({
      displayName: this.displayName,
      email: this.email,
      password: this.password,
      phone: this.phone,
      address: this.address,
      city: this.city,
      documentNumber: this.documentNumber,
      avatarUrl: this.avatarUrl,
    }).subscribe({
      next: () => {
        this.loading = false;
        this.router.navigateByUrl('/');
      },
      error: () => {
        this.loading = false;
        this.error = 'No se pudo registrar la cuenta.';
      },
    });
  }
}
