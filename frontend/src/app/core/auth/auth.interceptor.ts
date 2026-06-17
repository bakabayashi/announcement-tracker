import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';

/**
 * On a 401 from the API, send the user to the login page. The /me probe is excluded so the
 * auth guard can handle the unauthenticated case without a double redirect.
 */
export const authErrorInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);
  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401 && !req.url.includes('/api/v1/me')) {
        router.navigateByUrl('/login');
      }
      return throwError(() => error);
    }),
  );
};
