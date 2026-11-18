import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';

/**
 * Exige sessão válida (token guardado e não vencido).
 * Quando não autenticado, encaminha para `/entrar` preservando a URL de destino.
 */
export const authGuard: CanActivateFn = async (_route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  const usuario = await authService.obterUsuarioLogado();
  if (usuario) {
    return true;
  }

  const destino = state?.url;
  if (destino && destino !== '/' && destino !== '/entrar') {
    return router.createUrlTree(['/entrar'], { queryParams: { returnUrl: destino } });
  }

  return router.createUrlTree(['/entrar']);
};
