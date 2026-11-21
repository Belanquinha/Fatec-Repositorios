export interface UsuarioLogado {
  nome: string;
  email: string;
  foto?: string;
  role?: string;
  /**
   * ID da conta, quando a sessão entrega. Sem ele o front-end não consegue saber
   * se o card é do usuário — e aí a ação Excluir fica oculta (nunca advinhada).
   */
  id?: string;
}