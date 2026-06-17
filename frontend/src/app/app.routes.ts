import { Routes } from '@angular/router';
import { adminGuard, authGuard } from './core/auth/auth.guard';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./features/auth/login.component').then((m) => m.LoginComponent),
  },
  {
    path: '',
    loadComponent: () => import('./layout/layout.component').then((m) => m.LayoutComponent),
    canActivate: [authGuard],
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'listings' },
      {
        path: 'listings',
        loadComponent: () =>
          import('./features/listings/listings-list.component').then(
            (m) => m.ListingsListComponent,
          ),
      },
      {
        path: 'listings/:id',
        loadComponent: () =>
          import('./features/listings/listing-detail.component').then(
            (m) => m.ListingDetailComponent,
          ),
      },
      {
        path: 'saved',
        loadComponent: () =>
          import('./features/saved/saved-listings.component').then((m) => m.SavedListingsComponent),
      },
      {
        path: 'searches',
        loadComponent: () =>
          import('./features/search-criteria/search-criteria.component').then(
            (m) => m.SearchCriteriaComponent,
          ),
      },
      {
        path: 'duplicates',
        canActivate: [adminGuard],
        loadComponent: () =>
          import('./features/duplicates/duplicates.component').then((m) => m.DuplicatesComponent),
      },
    ],
  },
  { path: '**', redirectTo: '' },
];
