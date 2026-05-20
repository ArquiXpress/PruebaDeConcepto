import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { signal } from '@angular/core';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { AppComponent } from './app.component';
import { AuthService } from './services/auth.service';
import { CatalogService } from './services/catalog.service';
import { NotificationService } from './services/notification.service';
import { SessionService } from './services/session.service';

describe('AppComponent', () => {
  beforeEach(async () => {
    const sessionMock = {
      currentUser: signal(null),
      refreshFromBackend: jasmine.createSpy('refreshFromBackend'),
      hasRole: jasmine.createSpy('hasRole').and.returnValue(false),
      isLoggedIn: jasmine.createSpy('isLoggedIn').and.returnValue(false),
      isAdmin: jasmine.createSpy('isAdmin').and.returnValue(false),
      isOperator: jasmine.createSpy('isOperator').and.returnValue(false),
      isSeller: jasmine.createSpy('isSeller').and.returnValue(false),
      canAccessOperations: jasmine.createSpy('canAccessOperations').and.returnValue(false),
      isLogisticsOnly: jasmine.createSpy('isLogisticsOnly').and.returnValue(false),
    };

    await TestBed.configureTestingModule({
      imports: [AppComponent],
      providers: [
        provideHttpClient(),
        provideRouter([]),
        { provide: SessionService, useValue: sessionMock },
        { provide: CatalogService, useValue: { search: () => of({ content: [] }) } },
        { provide: NotificationService, useValue: { unreadCount: () => of({ count: 0 }) } },
        { provide: AuthService, useValue: { logout: jasmine.createSpy('logout') } },
      ],
    }).compileComponents();
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(AppComponent);
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
  });

  it('should start with an empty search term', () => {
    const fixture = TestBed.createComponent(AppComponent);
    const app = fixture.componentInstance;
    expect(app.searchTerm).toBe('');
  });

  it('should render the ArquiXpress brand', () => {
    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.brand strong')?.textContent).toContain('ArquiXpress');
  });
});
