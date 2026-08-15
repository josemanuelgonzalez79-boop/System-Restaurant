import { Routes } from '@angular/router';

import { adminGuard, authGuard, ownerGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./features/auth/login/login').then((component) => component.Login),
    title: 'Iniciar sesión | Essential',
  },
  {
    path: 'setup',
    loadComponent: () => import('./features/auth/setup/setup').then((component) => component.Setup),
    title: 'Configurar | Essential',
  },
  {
    path: '',
    loadComponent: () => import('./features/home/home').then((component) => component.Home),
    canActivate: [authGuard],
    title: 'Inicio | Essential',
  },
  {
    path: 'pedidos/:id/cobro',
    loadComponent: () =>
      import('./features/payment/payment').then((component) => component.PaymentPage),
    canActivate: [authGuard],
    title: 'Cobro del pedido | Essential',
  },
  {
    path: 'pedidos/:id',
    loadComponent: () =>
      import('./features/order-detail/order-detail').then((component) => component.OrderDetailPage),
    canActivate: [authGuard],
    title: 'Capturar pedido | Essential',
  },
  {
    path: 'pedidos',
    loadComponent: () => import('./features/orders/orders').then((component) => component.Orders),
    canActivate: [authGuard],
    title: 'Mesas y pedidos | Essential',
  },
  {
    path: 'preparacion',
    loadComponent: () =>
      import('./features/preparation/preparation').then((component) => component.PreparationPage),
    canActivate: [authGuard],
    title: 'Preparación | Essential',
  },
  {
    path: 'catalogo',
    loadComponent: () =>
      import('./features/catalog/catalog').then((component) => component.Catalog),
    canActivate: [adminGuard],
    title: 'Catálogo | Essential',
  },
  {
    path: 'estructura',
    loadComponent: () =>
      import('./features/structure/structure').then((component) => component.Structure),
    canActivate: [adminGuard],
    title: 'Estructura | Essential',
  },
  {
    path: 'usuarios',
    loadComponent: () => import('./features/users/users').then((component) => component.Users),
    canActivate: [ownerGuard],
    title: 'Usuarios | Essential',
  },
  {
    path: 'configuracion',
    loadComponent: () =>
      import('./features/settings/settings').then((component) => component.Settings),
    canActivate: [adminGuard],
    title: 'Configuración | Essential',
  },
  {
    path: '**',
    loadComponent: () =>
      import('./features/not-found/not-found').then((component) => component.NotFound),
    title: 'Página no encontrada | Essential',
  },
];
