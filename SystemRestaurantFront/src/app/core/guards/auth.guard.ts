import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { map } from 'rxjs';

import { AuthApiService } from '../services/auth-api.service';

export const authGuard: CanActivateFn = () => {
  const auth = inject(AuthApiService);
  const router = inject(Router);

  return auth.ensureInitialized().pipe(
    map(() => {
      if (auth.setupRequired()) {
        return router.createUrlTree(['/setup']);
      }
      return auth.authenticated() ? true : router.createUrlTree(['/login']);
    }),
  );
};

export const adminGuard: CanActivateFn = () => {
  const auth = inject(AuthApiService);
  const router = inject(Router);

  return auth.ensureInitialized().pipe(
    map(() => {
      if (auth.setupRequired()) {
        return router.createUrlTree(['/setup']);
      }
      if (!auth.authenticated()) {
        return router.createUrlTree(['/login']);
      }
      return auth.canAdminister() ? true : router.createUrlTree(['/']);
    }),
  );
};

export const ownerGuard: CanActivateFn = () => {
  const auth = inject(AuthApiService);
  const router = inject(Router);

  return auth.ensureInitialized().pipe(
    map(() => {
      if (auth.setupRequired()) {
        return router.createUrlTree(['/setup']);
      }
      if (!auth.authenticated()) {
        return router.createUrlTree(['/login']);
      }
      return auth.isOwner() ? true : router.createUrlTree(['/']);
    }),
  );
};
