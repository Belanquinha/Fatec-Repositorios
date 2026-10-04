/**
 * Atalho de login de desenvolvimento, oferecido pelo back-end em `/auth/dev-login/contas`.
 *
 * A lista de e-mails vem do back-end, e não deste lado: a regra de quem é aluno e quem é professor
 * mora no back-end, e duplicá-la aqui faria o botão oferecer um papel diferente do que o login
 * realmente concede.
 */
export interface ContaDev {
  email: string;
  rotulo: string;
}