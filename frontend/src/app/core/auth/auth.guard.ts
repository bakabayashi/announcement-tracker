import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, map, of } from 'rxjs';
import { AuthService } from './auth.service';

/** Allows the route only for an authenticated user; otherwise sends to /login. */
export const authGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);

  if (auth.isAuthenticated()) {
    return true;
  }
  return auth.loadCurrentUser().pipe(
    map(() => true),
    catchError(() => of(router.parseUrl('/login'))),
  );
};

/** Allows the route only for an ADMIN; non-admins go to /listings, anonymous to /login. */
export const adminGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);

  const decide = () => (auth.isAdmin() ? true : router.parseUrl('/listings'));

  if (auth.isAuthenticated()) {
    return decide();
  }
  return auth.loadCurrentUser().pipe(
    map(decide),
    catchError(() => of(router.parseUrl('/login'))),
  );
};
