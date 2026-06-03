import { HttpInterceptorFn, HttpStatusCode } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';
import { catchError } from 'rxjs/operators';
import { throwError } from 'rxjs';

/**
 * HTTP interceptor that:
 * 1. Attaches the in-memory Bearer token to every authenticated request.
 * 2. Attaches X-Business-Id for tenant-scoped endpoints.
 * 3. Logs out the user on 401 (expired/invalid token) OR 403 (forbidden).
 *
 * 401 vs 403:
 * - 401 Unauthorized → the token is missing, expired, or invalid. Logout + redirect.
 * - 403 Forbidden    → the token is valid but the user lacks permission. Also logout to
 *   prevent a confused UI state (e.g. a role change on the server side).
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const isAuthEndpoint = req.url.includes('/api/v1/auth/');

  if (isAuthEndpoint) {
    return next(req);
  }

  const token = auth.getToken();
  if (!token) return next(req);

  const headers: Record<string, string> = { Authorization: `Bearer ${token}` };
  const businessId = auth.getActiveBusinessId();
  const isBusinessScoped = req.url.includes('/api/v1/businesses/');
  if (businessId && isBusinessScoped) {
    headers['X-Business-Id'] = businessId;
  }

  return next(req.clone({ setHeaders: headers })).pipe(
    catchError((err) => {
      if (err.status === HttpStatusCode.Unauthorized || err.status === HttpStatusCode.Forbidden) {
        // Clear in-memory token and session metadata, then redirect to login.
        auth.logout();
      }
      return throwError(() => err);
    })
  );
};
