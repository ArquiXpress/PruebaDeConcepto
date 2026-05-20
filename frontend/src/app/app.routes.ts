import { inject } from '@angular/core';
import { CanActivateFn, Router, Routes } from '@angular/router';
import { HomeComponent } from './pages/home/home.component';
import { CatalogPageComponent } from './pages/catalog-page/catalog-page.component';
import { CartPageComponent } from './pages/cart-page/cart-page.component';
import { CheckoutPageComponent } from './pages/checkout-page/checkout-page.component';
import { CheckoutResultPageComponent } from './pages/checkout-result-page/checkout-result-page.component';
import { LoginPageComponent } from './pages/login-page/login-page.component';
import { RegisterPageComponent } from './pages/register-page/register-page.component';
import { OperationsPageComponent } from './pages/operations-page/operations-page.component';
import { PasswordRecoveryPageComponent } from './pages/password-recovery-page/password-recovery-page.component';
import { ProfilePageComponent } from './pages/profile-page/profile-page.component';
import { ProductDetailPageComponent } from './pages/product-detail-page/product-detail-page.component';
import { PurchasesPageComponent } from './pages/purchases-page/purchases-page.component';
import { FavoritesPageComponent } from './pages/favorites-page/favorites-page.component';
import { PqrsPageComponent } from './pages/pqrs-page/pqrs-page.component';
import { LogisticsHubPageComponent } from './pages/logistics-hub-page/logistics-hub-page.component';
import { SellerPortalComponent } from './pages/seller-portal/seller-portal.component';
import { NotificationsPageComponent } from './pages/notifications-page/notifications-page.component';
import { SellerProductDetailPageComponent } from './pages/seller-product-detail-page/seller-product-detail-page.component';
import { SessionService } from './services/session.service';

const redirectLogisticsOnly: CanActivateFn = () => {
  const session = inject(SessionService);
  if (session.isLogisticsOnly()) {
    return inject(Router).createUrlTree(['/logistica']);
  }
  return true;
};

export const routes: Routes = [
  { path: '', component: HomeComponent, canActivate: [redirectLogisticsOnly] },
  { path: 'login', component: LoginPageComponent, canActivate: [redirectLogisticsOnly] },
  { path: 'registro', component: RegisterPageComponent, canActivate: [redirectLogisticsOnly] },
  { path: 'recuperar-clave', component: PasswordRecoveryPageComponent, canActivate: [redirectLogisticsOnly] },
  { path: 'perfil', component: ProfilePageComponent, canActivate: [redirectLogisticsOnly] },
  { path: 'mis-compras', component: PurchasesPageComponent, canActivate: [redirectLogisticsOnly] },
  { path: 'favoritos', component: FavoritesPageComponent, canActivate: [redirectLogisticsOnly] },
  { path: 'notificaciones', component: NotificationsPageComponent, canActivate: [redirectLogisticsOnly] },
  { path: 'pqrs', component: PqrsPageComponent, canActivate: [redirectLogisticsOnly] },
  { path: 'catalogo', component: CatalogPageComponent, canActivate: [redirectLogisticsOnly] },
  { path: 'producto/:id', component: ProductDetailPageComponent, canActivate: [redirectLogisticsOnly] },
  { path: 'carrito', component: CartPageComponent, canActivate: [redirectLogisticsOnly] },
  { path: 'checkout/resultado', component: CheckoutResultPageComponent, canActivate: [redirectLogisticsOnly] },
  { path: 'checkout', component: CheckoutPageComponent, canActivate: [redirectLogisticsOnly] },
  { path: 'operaciones', component: OperationsPageComponent, canActivate: [redirectLogisticsOnly] },
  { path: 'logistica', component: LogisticsHubPageComponent },
  { path: 'vendedor', component: SellerPortalComponent, canActivate: [redirectLogisticsOnly] },
  { path: 'vendedor/publicaciones/:id', component: SellerProductDetailPageComponent, canActivate: [redirectLogisticsOnly] },
];
