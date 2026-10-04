import {
  gravarSessao,
  lerChave,
  lerTokenValido,
  limparSessao,
} from './sessao-navegador';

/**
 * JWT com três segmentos e `exp` no payload. A assinatura é irrelevante aqui — o front-end não
 * valida assinatura, e quem valida é o back-end.
 */
function criarJwt(expEmSegundos: number): string {
  const payload = btoa(JSON.stringify({ exp: expEmSegundos }))
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
    .replace(/=+$/, '');
  return `header.${payload}.assinatura`;
}

const AGORA = () => Math.floor(Date.now() / 1000);

describe('sessao-navegador', () => {
  afterEach(() => {
    localStorage.clear();
  });

  describe('gravarSessao', () => {
    it('deve gravar todas as chaves devolvidas pelo back-end', () => {
      gravarSessao({
        accessToken: 'jwt',
        tokenType: 'Bearer',
        expiresInSeconds: 86400,
        nome: 'Maria da CPS',
        email: 'maria@cps.sp.gov.br',
        fotoUrl: 'https://exemplo.test/foto.png',
        role: 'PROFESSOR',
      });

      expect(localStorage.getItem('accessToken')).toBe('jwt');
      expect(localStorage.getItem('tokenType')).toBe('Bearer');
      expect(localStorage.getItem('expiresInSeconds')).toBe('86400');
      expect(localStorage.getItem('usuarioNome')).toBe('Maria da CPS');
      expect(localStorage.getItem('usuarioEmail')).toBe('maria@cps.sp.gov.br');
      expect(localStorage.getItem('usuarioFoto')).toBe('https://exemplo.test/foto.png');
      expect(localStorage.getItem('usuarioRole')).toBe('PROFESSOR');
    });

    it('não deve gravar nada quando a resposta não traz token', () => {
      gravarSessao({ nome: 'Sem token' });

      // Gravar o nome sem o token produziria o estado "perfil visível, nenhuma requisição
      // autenticada" — a mesma mentira que a checagem de validade existe para impedir.
      expect(localStorage.getItem('usuarioNome')).toBeNull();
    });

    it('não deve lançar quando a resposta é null ou indefinida', () => {
      expect(() => gravarSessao(null)).not.toThrow();
      expect(() => gravarSessao(undefined)).not.toThrow();
    });

    it('deve assumir Bearer quando o back-end não informa o tipo', () => {
      gravarSessao({ accessToken: 'jwt' });

      expect(localStorage.getItem('tokenType')).toBe('Bearer');
    });
  });

  describe('limparSessao', () => {
    it('deve remover as chaves de sessão e preservar as demais', () => {
      localStorage.setItem('accessToken', 'jwt');
      localStorage.setItem('tokenType', 'Bearer');
      localStorage.setItem('expiresInSeconds', '3600');
      localStorage.setItem('usuarioNome', 'Aluno');
      localStorage.setItem('usuarioEmail', 'aluno@aluno.cps.sp.gov.br');
      localStorage.setItem('usuarioFoto', 'foto');
      localStorage.setItem('usuarioRole', 'ADMIN');
      localStorage.setItem('msal.cache-id-token', 'cache do MSAL');
      localStorage.setItem('outra-chave', 'deve-sobreviver');

      limparSessao();

      expect(localStorage.getItem('accessToken')).toBeNull();
      expect(localStorage.getItem('tokenType')).toBeNull();
      expect(localStorage.getItem('expiresInSeconds')).toBeNull();
      expect(localStorage.getItem('usuarioNome')).toBeNull();
      expect(localStorage.getItem('usuarioEmail')).toBeNull();
      expect(localStorage.getItem('usuarioFoto')).toBeNull();
      expect(localStorage.getItem('usuarioRole')).toBeNull();
      // O cache do MSAL é de outra natureza e tem ciclo de vida próprio: apagá-lo aqui expulsaria a
      // pessoa da Microsoft e provocaria o MFA que este recurso existe para evitar.
      expect(localStorage.getItem('msal.cache-id-token')).toBe('cache do MSAL');
      expect(localStorage.getItem('outra-chave')).toBe('deve-sobreviver');
    });
  });

  describe('lerTokenValido', () => {
    it('deve devolver o token quando ainda não venceu', () => {
      const token = criarJwt(AGORA() + 3600);
      localStorage.setItem('accessToken', token);

      expect(lerTokenValido()).toBe(token);
    });

    it('deve devolver undefined e descartar a sessão quando o token venceu', () => {
      localStorage.setItem('accessToken', criarJwt(AGORA() - 1));
      localStorage.setItem('usuarioNome', 'Aluno');

      expect(lerTokenValido()).toBeUndefined();
      expect(localStorage.getItem('accessToken')).toBeNull();
      expect(localStorage.getItem('usuarioNome')).toBeNull();
    });

    it('deve tratar o instante exato da expiração como vencido', () => {
      localStorage.setItem('accessToken', criarJwt(AGORA()));

      // `>=` e não `>`: no instante do exp o token já não vale.
      expect(lerTokenValido()).toBeUndefined();
    });

    it('deve devolver undefined quando não há token algum', () => {
      expect(lerTokenValido()).toBeUndefined();
    });

    it('deve tratar string vazia como ausente', () => {
      localStorage.setItem('accessToken', '');

      expect(lerTokenValido()).toBeUndefined();
    });

    it.each([
      ['token sem segmentos', 'nao-e-jwt'],
      ['token de dois segmentos', 'header.payload'],
      ['payload que não é JSON', 'header.nao-e-json.assinatura'],
      ['payload sem exp', `header.${btoa('{"sub":"x"}')}.assinatura`],
      ['exp que não é número', `header.${btoa('{"exp":"amanha"}')}.assinatura`],
    ])('deve tratar %s como vencido, sem lançar', (_caso, token) => {
      localStorage.setItem('accessToken', token);

      expect(() => lerTokenValido()).not.toThrow();
      expect(lerTokenValido()).toBeUndefined();
    });

    it('deve decodificar payload base64url, com - e _ no lugar de + e /', () => {
      // O alphabet base64url é o que o JWT usa. Decodificar como base64 estrito faria o payload
      // virar lixo, o `JSON.parse` falhar, e toda sessão válida ser tratada como vencida.
      const payloadOriginal = { exp: AGORA() + 3600, sub: 'ç-ÿ' };
      const base64 = btoa(JSON.stringify(payloadOriginal));
      const base64url = base64.replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
      expect(base64url).toMatch(/^[A-Za-z0-9_-]+$/);

      localStorage.setItem('accessToken', `header.${base64url}.assinatura`);

      expect(lerTokenValido()).toBe(`header.${base64url}.assinatura`);
    });

    it('deve decodificar payload sem padding, comum em JWT', () => {
      // `atob` exige comprimento múltiplo de 4. O base64 só sai sem padding quando o número de
      // bytes é múltiplo de 3, então o payload abaixo tem 25 bytes — e precisa de `==`.
      const payload = btoa(JSON.stringify({ exp: AGORA() + 3600, s: 'x' }))
        .replace(/\+/g, '-')
        .replace(/\//g, '_')
        .replace(/=+$/, '');
      expect(payload.length % 4).not.toBe(0);

      localStorage.setItem('accessToken', `header.${payload}.assinatura`);

      expect(lerTokenValido()).not.toBeUndefined();
    });

    it('deve ler payload UTF-8 com acento sem falhar a decodificação', () => {
      // `atob` devolve bytes latin-1. Sem o `TextDecoder` um nome como "João" viraria lixo e o
      // `JSON.parse` lançaria, tratando uma sessão legítima como vencida.
      const json = JSON.stringify({ exp: AGORA() + 3600, nome: 'João da Silva' });
      const base64 = btoa(String.fromCharCode(...new TextEncoder().encode(json)));

      localStorage.setItem('accessToken', `header.${base64}.assinatura`);

      expect(lerTokenValido()).not.toBeUndefined();
    });
  });

  describe('lerChave', () => {
    it('deve devolver null para chave ausente', () => {
      expect(lerChave('nao-existe')).toBeNull();
    });
  });
});