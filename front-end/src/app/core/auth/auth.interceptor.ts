import { HttpInterceptorFn } from '@angular/common/http';
import { catchError, throwError } from 'rxjs';
import { lerChave, limparSessao } from './sessao-navegador';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const token = lerChave('accessToken');

  const requisicao = token
    ? req.clone({
        setHeaders: {
          Authorization: `Bearer ${token}`,
        },
      })
    : req;

  return next(requisicao).pipe(
    catchError((erro: unknown) => {
      // Um 401 significa que o back-end recusou o token: expirado, revogado ou assinado com outro
      // segredo. A sessão guardada no navegador é inservível, e mantê-la faria a UI continuar
      // anunciando um login cujas chamadas só falham — "logado" na tela e 401 em tudo. Descartar
      // aqui também faz o header cair para "deslogado" sem exigir F5.
      //
      // 403 não entra: é "autenticado, mas não autorizado", resposta legítima para um professor
      // que tenta aprovar um projeto de outra instituição. Limpar a sessão nesse caso expulsaria a
      // pessoa de um erro que ela pode corrigir sozinha.
      if ((erro as { status?: number } | null)?.status === 401) {
        limparSessao();
      }

      // O erro segue como estava: o tratamento de erro de quem chamou continua funcionando igual.
      return throwError(() => erro);
    })
  );
};