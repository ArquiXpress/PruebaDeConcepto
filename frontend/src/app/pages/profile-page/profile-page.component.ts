import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { SessionService } from '../../services/session.service';

@Component({
  selector: 'app-profile-page',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './profile-page.component.html',
  styleUrl: './profile-page.component.scss',
})
export class ProfilePageComponent implements OnInit {
  displayName = '';
  email = '';
  avatarUrl = '';
  phone = '';
  address = '';
  city = '';
  documentNumber = '';
  loading = false;
  success = '';
  error = '';

  constructor(
    private readonly auth: AuthService,
    public readonly session: SessionService
  ) {}

  ngOnInit(): void {
    const user = this.session.currentUser();
    if (!user) {
      return;
    }
    this.displayName = user.displayName;
    this.email = user.email;
    this.avatarUrl = user.avatarUrl ?? '';
    this.phone = user.phone ?? '';
    this.address = user.address ?? '';
    this.city = user.city ?? '';
    this.documentNumber = user.documentNumber ?? '';
  }

  save(): void {
    this.loading = true;
    this.success = '';
    this.error = '';
    this.auth.updateProfile({
      displayName: this.displayName,
      email: this.email,
      avatarUrl: this.avatarUrl,
      phone: this.phone,
      address: this.address,
      city: this.city,
      documentNumber: this.documentNumber,
    }).subscribe({
      next: () => {
        this.loading = false;
        this.success = 'Perfil actualizado correctamente.';
      },
      error: () => {
        this.loading = false;
        this.error = 'No se pudo actualizar el perfil.';
      },
    });
  }
}
