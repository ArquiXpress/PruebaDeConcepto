import { Routes } from '@angular/router';
import { HomeComponent } from './pages/home/home.component';
import { CatalogPageComponent } from './pages/catalog-page/catalog-page.component';
import { CartPageComponent } from './pages/cart-page/cart-page.component';
import { CheckoutPageComponent } from './pages/checkout-page/checkout-page.component';
import { LoginPageComponent } from './pages/login-page/login-page.component';
import { RegisterPageComponent } from './pages/register-page/register-page.component';
import { OperationsPageComponent } from './pages/operations-page/operations-page.component';
import { SellerPortalComponent } from './pages/seller-portal/seller-portal.component';

export const routes: Routes = [
  { path: '', component: HomeComponent },
  { path: 'login', component: LoginPageComponent },
  { path: 'registro', component: RegisterPageComponent },
  { path: 'catalogo', component: CatalogPageComponent },
  { path: 'carrito', component: CartPageComponent },
  { path: 'checkout', component: CheckoutPageComponent },
  { path: 'operaciones', component: OperationsPageComponent },
  { path: 'vendedor', component: SellerPortalComponent },
];