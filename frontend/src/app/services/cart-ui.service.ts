import { Injectable, signal } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class CartUIService {
  readonly isOpen = signal(false);

  toggle(): void {
    this.isOpen.set(!this.isOpen());
  }

  open(): void {
    this.isOpen.set(true);
  }

  close(): void {
    this.isOpen.set(false);
  }
}
