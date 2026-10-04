import { readFileSync } from 'node:fs';
import { join } from 'node:path';
import { environment } from './environment';

/**
 * O front-end não lê mais nada de variável de ambiente — estes testes existem para
 * travar essa decisão. Se alguém reintroduzir um `import.meta.env` com default,
 * estes testes falham.
 *
 * <b>Atenção ao que este arquivo consegue e o que não consegue verificar.</b> O runner de testes
 * (`@angular/build:unit-test`) sobe com a configuração `development`, e portanto com o
 * `fileReplacements` aplicado: o `import { environment }` abaixo resolve para
 * `environment.development.ts`, nunca para o de produção. Isso foi medido, não suposto — o
 * `apiUrl` observado nos testes é `http://localhost:4040`, e `production` é `false`.
 *
 * Consequência prática: nenhuma asserção sobre o objeto importado pode proteger o build de
 * produção. Para os invariantes que só existem lá, este arquivo lê o `environment.ts` como texto.
 */
describe('environment', () => {
  /**
   * Lê o `environment.ts` de produção como texto. O runner resolve `import.meta.url` para uma URL
   * que não é `file:`, então o caminho vem do diretório de trabalho do processo — que é a raiz do
   * projeto Angular. Um `cwd` inesperado faria o `readFileSync` lançar, e é o comportamento
   * desejado: melhor quebrar o teste do que passar em silêncio por não ter lido nada.
   */
  function lerEnvironmentDeProducao(): string {
    return readFileSync(join(process.cwd(), 'src/environments/environment.ts'), 'utf-8');
  }

  it('deve ter a URL da API como constante, sem fallback de import.meta.env', () => {
    // Os únicos valores válidos: com proxy (nginx) ou sem proxy (ng serve).
    expect(['/api', 'http://localhost:4040']).toContain(environment.apiUrl);
  });

  it('nao deve expor um prefixo /api duplicado na URL da API', () => {
    // `projeto.service` concatena `${apiUrl}/projetos`. Um default já terminado em
    // barra produziria URLs com barra dupla.
    expect(environment.apiUrl.endsWith('/')).toBe(false);
  });

  it('deve derivar o redirect URI da pagina real, e nao de um host fixo', () => {
    // O Entra ID exige que a URI esteja registrada (AADSTS50011 caso contrário).
    // Fixar `localhost:4200` fazia o login falhar em qualquer outro host.
    expect(environment.msalRedirectUri).toBe(document.baseURI);
  });

  it('deve usar o client id do app registration, nunca string vazia', () => {
    // String vazia só falha no navegador, com erro opaco do MSAL.
    expect(environment.msalClientId).toMatch(/^[0-9a-f-]{36}$/);
  });

  it('deve usar o tenant da CPS, nunca "common"', () => {
    expect(environment.msalAuthority).toBe(
      'https://login.microsoftonline.com/eabe64c5-68f5-4a76-8301-9577a679e449'
    );
  });

  it('deve ter production como booleano', () => {
    expect(typeof environment.production).toBe('boolean');
  });

  it('nao deve habilitar o login de desenvolvimento no build de producao', () => {
    // Lendo o arquivo, não importando: o objeto importado acima é o de desenvolvimento, e uma
    // asserção sobre ele passaria mesmo com a flag ligada em produção — que é exatamente o
    // cenário que precisa ser coberto.
    //
    // Com a flag em `true` no arquivo de produção, os atalhos "Entrar como aluno/professor/admin"
    // entram no bundle que vai para o ar. O back-end também fecha a rota, mas o botão não
    // deveria nem aparecer para quem não pode usá-lo.
    expect(lerEnvironmentDeProducao()).toMatch(/devAuthEnabled:\s*false/);
  });

  it('deve habilitar o login de desenvolvimento no build de desenvolvimento', () => {
    // Simétrico do anterior: o `ng serve` é onde a flag precisa estar ligada, ou o recurso
    // existiria só no back-end.
    expect(environment.devAuthEnabled).toBe(true);
  });
});
