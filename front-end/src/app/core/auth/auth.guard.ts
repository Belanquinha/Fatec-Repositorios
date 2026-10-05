import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';

/**
 * Exige sessão válida (token guardado e não vencido).
 *
 * Não passa pelo MSAL de propósito: `obterUsuarioLogado` só retorna usuário
 * quando há token de acesso válido no navegador, então é a prova de sessão
 * sem precisar inicializar a instância do Microsoft.
 */
export const authGuard: CanActivateFn = async () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  const usuario = await authService.obterUsuarioLogado();
  if (usuario) {
    return true;
  }

  return router.createUrlTree(['/']);
};
