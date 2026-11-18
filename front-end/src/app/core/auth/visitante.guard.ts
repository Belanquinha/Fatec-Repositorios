import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';

/**
 * Rota exclusiva para visitantes (ex.: `/entrar`).
 * Se a pessoa já possui sessão válida, redireciona para a página inicial.
 */
export const visitanteGuard: CanActivateFn = async () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  const usuario = await authService.obterUsuarioLogado();
  if (usuario) {
    return router.createUrlTree(['/']);
  }

  return true;
};
