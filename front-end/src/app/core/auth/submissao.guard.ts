import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';

/**
 * Libera a tela de submissão de projeto somente para ALUNO e PROFESSOR.
 *
 * ADMIN é o papel de gestão do catálogo (CRUD de instituições) e não tem fluxo
 * de publicação — sem isto a rota `/projeto-forms` ficava aberta para quem
 * tivesse qualquer sessão. O back-end também barra (`POST /projetos` exige
 * `ALUNO` ou `PROFESSOR`), mas a rota não deve nem renderizar para quem vai
 * receber 403.
 */
export const submissaoGuard: CanActivateFn = async () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  const usuario = await authService.obterUsuarioLogado();
  if (usuario?.role === 'ALUNO' || usuario?.role === 'PROFESSOR') {
    return true;
  }

  return router.createUrlTree(['/']);
};
