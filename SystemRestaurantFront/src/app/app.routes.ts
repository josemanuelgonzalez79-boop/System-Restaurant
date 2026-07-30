import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./features/home/home').then((component) => component.Home),
    title: 'Inicio | RestaurantSystem',
  },
  {
    path: 'catalogo',
    loadComponent: () =>
      import('./features/catalog/catalog').then((component) => component.Catalog),
    title: 'Catálogo | RestaurantSystem',
  },
  {
    path: 'configuracion',
    loadComponent: () =>
      import('./features/settings/settings').then((component) => component.Settings),
    title: 'Configuración | RestaurantSystem',
  },
  {
    path: '**',
    loadComponent: () =>
      import('./features/not-found/not-found').then((component) => component.NotFound),
    title: 'Página no encontrada | RestaurantSystem',
  },
];
