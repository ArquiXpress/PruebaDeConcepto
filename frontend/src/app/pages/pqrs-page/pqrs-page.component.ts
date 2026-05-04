import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-pqrs-page',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './pqrs-page.component.html',
  styleUrl: './pqrs-page.component.scss',
})
export class PqrsPageComponent {
  requestType = 'PETICION';
  personType = 'NATURAL';
  documentType = 'CEDULA';
  documentNumber = '';
  fullName = '';
  email = '';
  phone = '';
  responseChannel = 'EMAIL';
  address = '';
  city = '';
  orderId = '';
  subject = '';
  facts = '';
  request = '';
  attachments = '';
  dataConsent = false;
  submitted = false;

  submit(): void {
    if (!this.dataConsent) {
      return;
    }
    this.submitted = true;
  }
}
