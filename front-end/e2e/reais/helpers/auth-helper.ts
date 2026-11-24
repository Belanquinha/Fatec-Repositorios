import { APIRequestContext, expect } from '@playwright/test';
import { obterCodigoVerificacaoDocker } from './docker-helper';

/**
 * Resolve a URL base correta da API (porta 4040 direta ou porta 4200/api via reverse proxy Nginx).
 */
export async function resolverBaseUrlApi(request: APIRequestContext): Promise<string> {
  try {
    const probe = await request.get('http://localhost:4040/auth/cadastro/iniciar').catch(() => null);
    if (!probe || probe.status() === 404) {
      return 'http://localhost:4200/api';
    }
    return 'http://localhost:4040';
  } catch {
    return 'http://localhost:4200/api';
  }
}

/** Login real por e-mail/senha (BCrypt + JWT) — o mesmo chamado pelo formulário. */
export async function loginUsuarioReal(
  request: APIRequestContext,
  email: string,
  senha: string,
): Promise<string> {
  const baseUrl = await resolverBaseUrlApi(request);
  const res = await request.post(`${baseUrl}/auth/login`, { data: { email, senha } });
  expect(res.ok()).toBeTruthy();
  const dados = await res.json();
  return dados.accessToken as string;
}

/**
 * Cria e confirma uma conta real diretamente no banco PostgreSQL via API do Spring Boot.
 *
 * @returns Os dados do usuário cadastrado incluindo o token JWT de acesso
 */
export async function cadastrarUsuarioReal(
  request: APIRequestContext,
  nome: string,
  email: string,
  senha: string,
): Promise<{ email: string; senha: string; nome: string; accessToken: string }> {
  const baseUrl = await resolverBaseUrlApi(request);

  // 1. Inicia o cadastro na API.
  //
  // Idempotente por necessidade: `beforeAll` roda uma vez por worker e os specs
  // reutilizam um e-mail fixo no arquivo, então a segunda execução (ou um run
  // repetido com o mesmo timestamp) encontra a conta já confirmada. Nesse caso
  // o back-end responde 409 "Este e-mail já possui cadastro" — a conta existe,
  // que é exatamente a premissa do helper. Só falha de fato quando o erro não
  // é esse.
  const resIniciar = await request.post(`${baseUrl}/auth/cadastro/iniciar`, {
    data: { nome, email, senha },
  });

  let codigo: string;
  if (resIniciar.ok()) {
    // Conta nova: captura o código de 6 dígitos gerado nos logs do Docker.
    codigo = await obterCodigoVerificacaoDocker(email);
  } else if (resIniciar.status() === 409) {
    // Conta já confirmada: o cooldown impede novo envio de código, mas o login
    // com a senha informada basta para provar que a conta está ativa.
    return autenticarExistente(request, baseUrl, email, senha, nome);
  } else {
    const corpo = await resIniciar.text().catch(() => '');
    throw new Error(
      `Falha ao iniciar o cadastro de "${email}" em ${baseUrl}: HTTP ${resIniciar.status()} - ${corpo}`,
    );
  }

  // 2. Confirma o cadastro com o código para persistir no banco e obter o JWT
  const resConfirmar = await request.post(`${baseUrl}/auth/cadastro/confirmar`, {
    data: { email, codigo },
  });
  expect(resConfirmar.ok()).toBeTruthy();

  const dados = await resConfirmar.json();
  return {
    email,
    senha,
    nome,
    accessToken: dados.accessToken,
  };
}

/** Conta já existente: faz login real e devolve a mesma forma de `cadastrarUsuarioReal`. */
async function autenticarExistente(
  request: APIRequestContext,
  baseUrl: string,
  email: string,
  senha: string,
  nome: string,
): Promise<{ email: string; senha: string; nome: string; accessToken: string }> {
  const resLogin = await request.post(`${baseUrl}/auth/login`, { data: { email, senha } });
  const corpo = resLogin.ok() ? null : await resLogin.text().catch(() => '');
  if (!resLogin.ok()) {
    throw new Error(
      `A conta "${email}" já existe mas a senha informada não autenticou ` +
        `(HTTP ${resLogin.status()}): ${corpo}`,
    );
  }

  const dados = await resLogin.json();
  return {
    email,
    senha,
    // O nome real da conta pode ter sido definido em outro run; o nome pedido é
    // mantido porque os testes só o usam em asserções de tela próprias.
    nome,
    accessToken: dados.accessToken as string,
  };
}
