import { CommonModule } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Product } from '../../models/product';
import { CartService } from '../../services/cart.service';
import { AuthService, LoginResponse } from '../../services/auth.service';
import { CatalogService } from '../../services/catalog.service';
import { CheckoutProduct, CheckoutResponse, CheckoutService } from '../../services/checkout.service';
import { SessionService } from '../../services/session.service';

const PAYMENT_METHOD_OPTIONS = [
  'Tarjeta de credito',
  'Tarjeta debito',
  'PSE',
  'Transferencia bancaria',
  'Pago contra entrega',
  'Otro',
] as const;

const PSE_BANK_OPTIONS = [
  'Bancolombia',
  'Banco de Bogota',
  'Davivienda',
  'BBVA',
  'Banco de Occidente',
  'Nequi',
  'Nu Colombia',
] as const;

type PaymentMethod = (typeof PAYMENT_METHOD_OPTIONS)[number];

interface DemoAdminTransferAccount {
  bank: string;
  accountType: string;
  accountNumber: string;
  holderName: string;
  document: string;
  supportEmail: string;
  referenceCode: string;
}

interface PaymentSummary {
  title: string;
  rows: Array<{ label: string; value: string }>;
}

@Component({
  selector: 'app-checkout-page',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './checkout-page.component.html',
  styleUrl: './checkout-page.component.scss',
})
export class CheckoutPageComponent implements OnInit {
  readonly paymentMethodOptions = PAYMENT_METHOD_OPTIONS;
  readonly pseBankOptions = PSE_BANK_OPTIONS;
  readonly expiryMonths = Array.from({ length: 12 }, (_, index) => String(index + 1).padStart(2, '0'));
  readonly expiryYears = Array.from({ length: 12 }, (_, index) => String(new Date().getFullYear() + index));
  result = signal<CheckoutResponse | null>(null);
  products: Product[] = [];
  paymentSummary: PaymentSummary | null = null;
  selectedPaymentMethod: PaymentMethod = PAYMENT_METHOD_OPTIONS[0];
  customPaymentMethod = '';
  cardHolderName = '';
  cardNumber = '';
  cardCvv = '';
  expiryMonth = '';
  expiryYear = '';
  installments = 1;
  pseBank: (typeof PSE_BANK_OPTIONS)[number] = PSE_BANK_OPTIONS[0];
  pseDocument = '';
  transferPayerName = '';
  transferReference = '';
  transferConfirmed = false;
  cashOnDeliveryNotes = '';
  shippingAddress = '';
  shippingCity = '';
  adminTransferAccount: DemoAdminTransferAccount = {
    bank: 'Bancolombia',
    accountType: 'Cuenta de ahorros',
    accountNumber: '0193 8874 5521',
    holderName: 'Marta Admin',
    document: 'NIT 900.345.210-7',
    supportEmail: 'admin@arquixpress.com',
    referenceCode: 'ARQX-ADMIN-2026',
  };
  errorMessage = '';
  loading = false;
  guestPromptOpen = false;

  constructor(
    public readonly cart: CartService,
    private readonly auth: AuthService,
    private readonly catalog: CatalogService,
    private readonly checkout: CheckoutService,
    public readonly session: SessionService
  ) {}

  ngOnInit(): void {
    this.guestPromptOpen = !this.session.isLoggedIn();
    const user = this.session.currentUser();
    this.shippingAddress = user?.address || '';
    this.shippingCity = user?.city || '';

    this.catalog.search('', '', 0, 1000).subscribe({
      next: (page) => {
        this.products = page.content;
      },
    });

    this.auth.listUsers().subscribe({
      next: (users) => this.applyAdminTransferAccount(users),
    });
  }

  itemCount(): number {
    return this.cart.items().reduce((sum, item) => sum + item.quantity, 0);
  }

  subtotal(): number {
    const result = this.result();
    if (result) {
      return result.total;
    }
    return this.cart.items().reduce((sum, item) => {
      const product = this.product(item.productId);
      return sum + (product?.price ?? 0) * item.quantity;
    }, 0);
  }

  estimatedShipping(): number {
    const result = this.result();
    if (result) {
      return result.shippingCost || 0;
    }
    return this.cart.items().length ? 12000 : 0;
  }

  summaryItems(): CheckoutProduct[] {
    const result = this.result();
    if (result) {
      return result.items;
    }
    return this.cart.items().map((item) => {
      const product = this.product(item.productId);
      const unitPrice = product?.price ?? 0;
      return {
        productId: item.productId,
        title: product?.title || item.productId,
        imageUrl: product?.imageUrl || '',
        quantity: item.quantity,
        unitPrice,
        subtotal: unitPrice * item.quantity,
      };
    });
  }

  product(productId: string): Product | undefined {
    return this.products.find((item) => item.id === productId);
  }

  paymentMethod(): string {
    return this.selectedPaymentMethod === 'Otro'
      ? this.customPaymentMethod.trim()
      : this.selectedPaymentMethod;
  }

  isCustomPaymentMethod(): boolean {
    return this.selectedPaymentMethod === 'Otro';
  }

  isCardMethod(): boolean {
    return this.selectedPaymentMethod === 'Tarjeta de credito' || this.selectedPaymentMethod === 'Tarjeta debito';
  }

  isCreditCardMethod(): boolean {
    return this.selectedPaymentMethod === 'Tarjeta de credito';
  }

  isPseMethod(): boolean {
    return this.selectedPaymentMethod === 'PSE';
  }

  isTransferMethod(): boolean {
    return this.selectedPaymentMethod === 'Transferencia bancaria';
  }

  isCashOnDeliveryMethod(): boolean {
    return this.selectedPaymentMethod === 'Pago contra entrega';
  }

  normalizeCardNumber(): void {
    const digits = this.cardNumber.replace(/\D/g, '').slice(0, 16);
    this.cardNumber = digits.replace(/(.{4})/g, '$1 ').trim();
  }

  normalizeCardCvv(): void {
    this.cardCvv = this.cardCvv.replace(/\D/g, '').slice(0, 4);
  }

  normalizePseDocument(): void {
    this.pseDocument = this.pseDocument.replace(/\D/g, '').slice(0, 10);
  }

  normalizeTransferReference(): void {
    this.transferReference = this.transferReference.toUpperCase().replace(/[^A-Z0-9-]/g, '').slice(0, 18);
  }

  cardBrand(): string {
    const digits = this.cardNumber.replace(/\D/g, '');
    if (!digits) {
      return 'Sin detectar';
    }
    if (digits.startsWith('4')) {
      return 'Visa';
    }
    if (/^5[1-5]/.test(digits) || /^2(2[2-9]|[3-6]|7[01])/.test(digits)) {
      return 'Mastercard';
    }
    if (/^3[47]/.test(digits)) {
      return 'American Express';
    }
    return 'Tarjeta bancaria';
  }

  onPaymentMethodChange(): void {
    this.errorMessage = '';
    this.paymentSummary = null;
    this.result.set(null);
  }

  formatStatus(value: string): string {
    return value
      .toLowerCase()
      .split('_')
      .map((segment) => segment.charAt(0).toUpperCase() + segment.slice(1))
      .join(' ');
  }

  statusClass(value: string): string {
    if (value === 'APPROVED' || value === 'PAID') {
      return 'is-success';
    }
    if (value === 'REJECTED' || value === 'PAYMENT_REJECTED' || value === 'ERROR') {
      return 'is-danger';
    }
    return 'is-warning';
  }

  executeCheckout(): void {
    if (!this.session.isLoggedIn()) {
      this.guestPromptOpen = true;
      return;
    }
    const items = this.cart.items();
    if (!items.length) {
      return;
    }
    const paymentMethod = this.paymentMethod();
    if (!paymentMethod) {
      this.errorMessage = 'Selecciona o ingresa un metodo de pago para continuar.';
      return;
    }

    const paymentValidationError = this.validatePaymentData(paymentMethod);
    if (paymentValidationError) {
      this.errorMessage = paymentValidationError;
      return;
    }

    this.errorMessage = '';
    this.paymentSummary = null;
    this.result.set(null);
    this.loading = true;
    if (!this.shippingAddress.trim() || !this.shippingCity.trim()) {
      this.errorMessage = 'Ingresa direccion y ciudad de envio para calcular la entrega.';
      return;
    }

    this.checkout.checkout(items, paymentMethod, this.shippingAddress.trim(), this.shippingCity.trim()).subscribe({
      next: (response) => {
        this.paymentSummary = this.buildPaymentSummary(paymentMethod);
        this.result.set(response);
        this.cart.clear();
        this.loading = false;
      },
      error: (error) => {
        this.errorMessage = error?.error?.message || 'No se pudo completar el checkout.';
        this.loading = false;
      },
    });
  }

  private applyAdminTransferAccount(users: LoginResponse[]): void {
    const admin = users.find((user) => user.roles.includes('ADMIN'));
    if (!admin) {
      return;
    }
    this.adminTransferAccount = {
      ...this.adminTransferAccount,
      holderName: admin.displayName,
      supportEmail: admin.email,
    };
  }

  private validatePaymentData(paymentMethod: string): string | null {
    if (this.isCardMethod()) {
      const cardDigits = this.cardNumber.replace(/\D/g, '');
      const cvvDigits = this.cardCvv.replace(/\D/g, '');
      if (!this.cardHolderName.trim()) {
        return 'Ingresa el nombre del titular de la tarjeta.';
      }
      if (cardDigits.length < 13 || cardDigits.length > 16) {
        return 'Ingresa un numero de tarjeta valido.';
      }
      if (!this.expiryMonth || !this.expiryYear) {
        return 'Selecciona la fecha de expiracion de la tarjeta.';
      }
      if (this.isExpired()) {
        return 'La tarjeta simulada no puede estar vencida.';
      }
      if (cvvDigits.length < 3 || cvvDigits.length > 4) {
        return 'Ingresa un CVV valido.';
      }
      return null;
    }

    if (this.isPseMethod()) {
      const documentDigits = this.pseDocument.replace(/\D/g, '');
      if (!this.pseBank) {
        return 'Selecciona un banco para continuar con PSE.';
      }
      if (documentDigits.length < 6 || documentDigits.length > 10) {
        return 'Ingresa una cedula valida para PSE.';
      }
      return null;
    }

    if (this.isTransferMethod()) {
      if (!this.transferPayerName.trim()) {
        return 'Ingresa el nombre de quien realiza la transferencia.';
      }
      if (this.transferReference.trim().length < 6) {
        return 'Ingresa una referencia de transferencia simulada valida.';
      }
      if (!this.transferConfirmed) {
        return 'Confirma que usaras la cuenta bancaria simulada del administrador.';
      }
      return null;
    }

    return null;
  }

  private isExpired(): boolean {
    if (!this.expiryMonth || !this.expiryYear) {
      return false;
    }
    const selectedYear = Number(this.expiryYear);
    const selectedMonth = Number(this.expiryMonth);
    const now = new Date();
    const currentYear = now.getFullYear();
    const currentMonth = now.getMonth() + 1;
    return selectedYear < currentYear || (selectedYear === currentYear && selectedMonth < currentMonth);
  }

  private buildPaymentSummary(paymentMethod: string): PaymentSummary {
    if (this.isCardMethod()) {
      const rows = [
        { label: 'Titular', value: this.cardHolderName.trim() },
        { label: 'Tarjeta', value: this.maskCardNumber() },
        { label: 'Marca', value: this.cardBrand() },
        { label: 'Expira', value: `${this.expiryMonth}/${this.expiryYear.slice(-2)}` },
      ];
      if (this.isCreditCardMethod()) {
        rows.push({ label: 'Cuotas', value: `${this.installments} cuota(s)` });
      }
      return {
        title: 'Autorizacion simulada de tarjeta',
        rows,
      };
    }

    if (this.isPseMethod()) {
      return {
        title: 'Debito PSE simulado',
        rows: [
          { label: 'Banco', value: this.pseBank },
          { label: 'Cedula', value: this.maskDocument(this.pseDocument) },
          { label: 'Titular', value: this.session.currentUser()?.displayName || 'Cliente ArquiXpress' },
        ],
      };
    }

    if (this.isTransferMethod()) {
      return {
        title: 'Transferencia bancaria registrada',
        rows: [
          { label: 'Cuenta destino', value: `${this.adminTransferAccount.bank} · ${this.adminTransferAccount.accountNumber}` },
          { label: 'Titular destino', value: this.adminTransferAccount.holderName },
          { label: 'Quien transfiere', value: this.transferPayerName.trim() },
          { label: 'Referencia', value: this.transferReference.trim() },
        ],
      };
    }

    if (this.isCashOnDeliveryMethod()) {
      return {
        title: 'Pago contra entrega programado',
        rows: [
          {
            label: 'Instrucciones',
            value: this.cashOnDeliveryNotes.trim() || 'Sin instrucciones adicionales para la entrega.',
          },
        ],
      };
    }

    return {
      title: 'Metodo de pago personalizado',
      rows: [
        { label: 'Metodo', value: paymentMethod },
      ],
    };
  }

  private maskCardNumber(): string {
    const digits = this.cardNumber.replace(/\D/g, '');
    const lastDigits = digits.slice(-4);
    return `**** **** **** ${lastDigits}`;
  }

  private maskDocument(value: string): string {
    const digits = value.replace(/\D/g, '');
    if (digits.length <= 4) {
      return digits;
    }
    return `${'*'.repeat(Math.max(0, digits.length - 4))}${digits.slice(-4)}`;
  }
}
