import { Injectable, signal } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class AdminUIService {
  readonly isOpen = signal(false);

  toggle(): void {
    this.isOpen.update((state) => !state);
  }

  open(): void {
    this.isOpen.set(true);
  }

  close(): void {
    this.isOpen.set(false);
  }
}
