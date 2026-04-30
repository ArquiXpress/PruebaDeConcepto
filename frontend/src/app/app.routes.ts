import { Routes } from '@angular/router';
import { HomeComponent } from './pages/home/home.component';
import { CatalogPageComponent } from './pages/catalog-page/catalog-page.component';
import { CartPageComponent } from './pages/cart-page/cart-page.component';
import { CheckoutPageComponent } from './pages/checkout-page/checkout-page.component';
import { LoginPageComponent } from './pages/login-page/login-page.component';
import { RegisterPageComponent } from './pages/register-page/register-page.component';
import { LogisticsHubPageComponent } from './pages/logistics-hub-page/logistics-hub-page.component';

export const routes: Routes = [
  { path: '', component: HomeComponent },
  { path: 'login', component: LoginPageComponent },
  { path: 'registro', component: RegisterPageComponent },
  { path: 'catalogo', component: CatalogPageComponent },
  { path: 'carrito', component: CartPageComponent },
  { path: 'checkout', component: CheckoutPageComponent },
  { path: 'logistica', component: LogisticsHubPageComponent },
];
