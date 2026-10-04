/**
 * Estado da sessão no `localStorage`.
 *
 * Isolado em um módulo sem Angular e sem MSAL de propósito: tanto o `AuthService` quanto o
 * interceptor HTTP precisam ler e descartar estas chaves, e o interceptor roda em um contexto em que
 * não se deve puxar o MSAL. Duas implementações da mesma lista de chaves é o jeito mais rápido de
 * uma sessão fantasma sobreviver a um logout — uma delas deixa de limpar e nada denuncia.
 */

/** Tudo que o back-end devolve no login e que a UI depois consulta. */
export interface SessaoBackend {
  accessToken?: string;
  tokenType?: string;
  expiresInSeconds?: number;
  nome?: string;
  email?: string;
  fotoUrl?: string;
  role?: string;
}

const CHAVES = [
  'accessToken',
  'tokenType',
  'expiresInSeconds',
  'usuarioNome',
  'usuarioEmail',
  'usuarioFoto',
  'usuarioRole',
] as const;

/**
 * Grava a sessão devolvida pelo back-end. Comum ao login Microsoft e ao de desenvolvimento, para
 * que os dois produzam exatamente o mesmo estado no navegador — nenhuma tela precisa saber por qual
 * das duas vias a pessoa entrou.
 */
export function gravarSessao(dados: unknown): void {
  const sessao = (dados ?? {}) as SessaoBackend;

  if (typeof window === 'undefined' || !sessao.accessToken) {
    return;
  }

  window.localStorage.setItem('accessToken', sessao.accessToken);
  window.localStorage.setItem('tokenType', sessao.tokenType ?? 'Bearer');
  window.localStorage.setItem('expiresInSeconds', String(sessao.expiresInSeconds ?? 0));
  if (sessao.nome) window.localStorage.setItem('usuarioNome', sessao.nome);
  if (sessao.email) window.localStorage.setItem('usuarioEmail', sessao.email);
  if (sessao.fotoUrl) window.localStorage.setItem('usuarioFoto', sessao.fotoUrl);
  if (sessao.role) window.localStorage.setItem('usuarioRole', sessao.role);
}

/** Descarta a identidade guardada no navegador. */
export function limparSessao(): void {
  if (typeof window === 'undefined') {
    return;
  }
  for (const chave of CHAVES) {
    window.localStorage.removeItem(chave);
  }
}

/**
 * Devolve o token guardado quando ele ainda vale, e `undefined` quando não vale.
 *
 * A chave existir no `localStorage` não é prova de nada: o JWT tem validade e o `localStorage` não
 * expira nada. Um token vencido é descartado, e não só ignorado — deixá-lo ali manteria a UI
 * anunciando um login que nenhuma requisição consegue usar.
 */
export function lerTokenValido(): string | undefined {
  const token = lerChave('accessToken');
  if (!token) {
    return undefined;
  }

  if (tokenVencido(token)) {
    limparSessao();
    return undefined;
  }

  return token;
}

export function lerChave(chave: string): string | null {
  if (typeof window === 'undefined') {
    return null;
  }
  return window.localStorage.getItem(chave);
}

/**
 * Lê o `exp` do JWT. A assinatura não é verificada aqui — quem valida é o back-end; isto é apenas
 * uma leitura de metadado, para não apresentar como logada uma sessão que o servidor já recusaria.
 */
function tokenVencido(token: string): boolean {
  const partes = token.split('.');
  if (partes.length !== 3) {
    return true;
  }

  try {
    // Base64url usa `-`/`_` e dispensa padding; `atob` só aceita base64 estrito.
    const base64 = partes[1].replace(/-/g, '+').replace(/_/g, '/');
    const comPadding = base64.padEnd(base64.length + ((4 - (base64.length % 4)) % 4), '=');
    // O payload é UTF-8 (o nome pode ter acento) e `atob` devolve bytes latin-1.
    const json = new TextDecoder().decode(
      Uint8Array.from(atob(comPadding), (caractere) => caractere.charCodeAt(0))
    );
    const exp = (JSON.parse(json) as { exp?: unknown }).exp;

    if (typeof exp !== 'number') {
      return true;
    }
    return Date.now() >= exp * 1000;
  } catch {
    // Token ilegível conta como vencido: na dúvida o estado é "deslogado", que é o estado em que
    // a pessoa ainda consegue agir para se recuperar.
    return true;
  }
}