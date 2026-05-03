import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-password-recovery-page',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './password-recovery-page.component.html',
  styleUrl: './password-recovery-page.component.scss',
})
export class PasswordRecoveryPageComponent {
  email = '';
  token = '';
  newPassword = '';
  loading = false;
  resetting = false;
  message = '';
  error = '';

  constructor(
    private readonly auth: AuthService,
    private readonly router: Router,
    route: ActivatedRoute
  ) {
    this.token = route.snapshot.queryParamMap.get('token') ?? '';
  }

  requestReset(): void {
    this.loading = true;
    this.error = '';
    this.message = '';
    this.auth.requestPasswordReset(this.email).subscribe({
      next: (response) => {
        this.loading = false;
        this.message = response.message;
      },
      error: () => {
        this.loading = false;
        this.error = 'No se pudo generar el token de recuperacion.';
      },
    });
  }

  confirmReset(): void {
    this.resetting = true;
    this.error = '';
    this.auth.confirmPasswordReset(this.token, this.newPassword).subscribe({
      next: () => {
        this.resetting = false;
        this.router.navigateByUrl('/perfil');
      },
      error: () => {
        this.resetting = false;
        this.error = 'No se pudo cambiar la clave.';
      },
    });
  }
}
