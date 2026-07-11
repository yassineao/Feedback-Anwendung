import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./features/dashboard/pages/dashboard/main-page.component')
        .then((component) => component.MainPageComponent),
    title: 'Feedback Hub'
  },
  {
    path: 'auth',
    loadComponent: () =>
      import('./features/auth/pages/auth-page/auth-page.component')
        .then((component) => component.AuthPageComponent),
    title: 'Sign in | Feedback Hub'
  },
  {
    path: '**',
    redirectTo: ''
  }
];
