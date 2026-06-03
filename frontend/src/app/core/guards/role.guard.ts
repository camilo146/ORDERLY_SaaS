import { inject } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const roleGuard: CanActivateFn = (route: ActivatedRouteSnapshot) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const required: string[] = route.data['roles'] ?? [];

  if (!auth.isAuthenticated) return router.createUrlTree(['/login']);
  if (!required.length)      return true;
  if (required.includes(auth.role ?? '')) return true;

  return router.createUrlTree(['/dashboard']);
};
