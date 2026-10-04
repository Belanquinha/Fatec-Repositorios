package com.fatecrepository.e2e;

import com.fatecrepository.model.User;
import com.fatecrepository.model.UserRole;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Stories 1.1 e 1.3 — Autenticação via MSAL restrito ao tenant CPS e manutenção de sessão.
 *
 * <p>AC 1.1: "um teste com token emitido fora do tenant CPS é rejeitado no back-end (401)"
 * AC 1.1: "após autenticar como conta do tenant CPS, recebo sessão válida e acesso à aplicação"
 * AC 1.3: "acesso um recurso protegido com token válido / Then o back-end libera a requisição"
 * AC 1.3: "ao fazer logout, recursos protegidos passam a retornar 401 sem token novo"
 *
 * <p>O provedor Microsoft externo não é exercitado aqui (isso exigiria a rede e está
 * coberto por {@code MicrosoftTokenVerifierTest}). O que esta suíte garante é o
 * contrato HTTP do nosso endpoint de login e a validade do token emitido.
 */
@DisplayName("Stories 1.1 / 1.3 — Login Microsoft e sessão")
class AutenticacaoSessaoE2ETest extends ApiE2ETestSupport {

    @Nested
    @DisplayName("Contrato do endpoint POST /auth/login-microsoft")
    class ContratoDoLogin {

        @Test
        @DisplayName("Token Microsoft ausente devolve 400 com erro de validação por campo")
        void tokenAusenteDevolve400() {
            ResponseEntity<String> response = post("/auth/login-microsoft", Map.of(), null);

            assertThat(response.getStatusCode().value()).isEqualTo(400);
            JsonNode erros = json(response).path("erros");
            assertThat(erros.has("accessToken")).isTrue();
        }

        @Test
        @DisplayName("Token Microsoft em branco devolve 400")
        void tokenEmBrancoDevolve400() {
            ResponseEntity<String> response = post("/auth/login-microsoft",
                Map.of("accessToken", "   "), null);

            assertThat(response.getStatusCode().value()).isEqualTo(400);
        }

        @Test
        @DisplayName("Token Microsoft malformado devolve 401 e não 500")
        void tokenMalformadoDevolve401() {
            ResponseEntity<String> response = post("/auth/login-microsoft",
                Map.of("accessToken", "isto-nao-e-um-jwt"), null);

            assertNaoAutorizado(response);
            assertThat(texto(response, "mensagem")).contains("inválido");
        }

        @Test
        @DisplayName("O endpoint de login é público (não exige sessão para ser chamado)")
        void loginEhPublico() {
            // Se o login exigisse token, caímos em 401 do filtro em vez de 400 do bean validation.
            ResponseEntity<String> response = post("/auth/login-microsoft", Map.of(), null);

            assertThat(response.getStatusCode().value()).isEqualTo(400);
        }
    }

    @Nested
    @DisplayName("Story 1.3 — sessão válida libera recurso protegido")
    class SessaoValidaLiberaRecurso {

        @Test
        @DisplayName("Token emitido para aluno abre GET /projetos/meus")
        void tokenDeAlunoAbreMeusProjetos() {
            User aluno = criarUsuario("Ana Aluna", "ana.aluna@aluno.cps.sp.gov.br", UserRole.ALUNO);

            ResponseEntity<String> response = get("/projetos/meus", tokenDe(aluno));

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody()).isEqualTo("[]");
        }

@Test
        @DisplayName("Token com assinatura adulterada devolve 401")
        void tokenAdulteradoDevolve401() {
            User aluno = criarUsuario("Bruno Alterado", "bruno.alterado@aluno.cps.sp.gov.br", UserRole.ALUNO);
            String tokenValido = tokenDe(aluno);
            String tokenAdulterado = tokenValido.substring(0, tokenValido.lastIndexOf('.') + 1) + "aaa";

            assertNaoAutorizado(get("/projetos/meus", tokenAdulterado));
        }

        @Test
        @DisplayName("Token de usuário removido do banco devolve 401 (sessão órfã)")
        void tokenDeUsuarioRemovidoDevolve401() {
            User temporario = criarUsuario("Carla Temporária", "carla.temporaria@aluno.cps.sp.gov.br", UserRole.ALUNO);
            String token = tokenDe(temporario);

            // O token continua criptograficamente válido, mas o usuário sumiu.
            userRepository.deleteById(temporario.getId());
            userRepository.flush();

            assertNaoAutorizado(get("/projetos/meus", token));
        }

        @Test
        @DisplayName("Story 1.3 — após logout (token descartado), o recurso protegido volta a 401")
        void aposLogoutRecursoVolta401() {
            User aluno = criarUsuario("Diego Logout", "diego.logout@aluno.cps.sp.gov.br", UserRole.ALUNO);
            String token = tokenDe(aluno);

            // Antes do logout: 200.
            assertThat(get("/projetos/meus", token).getStatusCode().value()).isEqualTo(200);

            // Logout no front-end = descartar o token. Sem token novo, o back-end nega.
            assertNaoAutorizado(get("/projetos/meus"));
        }

        @Test
        @DisplayName("Header Authorization sem o prefixo 'Bearer ' é ignorado")
        void headerSemBearerEhIgnorado() {
            User aluno = criarUsuario("Elis Prefixo", "elis.prefixo@aluno.cps.sp.gov.br", UserRole.ALUNO);

            // Token válido, mas enviado cru: o filtro exige o prefixo "Bearer ".
            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.AUTHORIZATION, tokenDe(aluno));

            ResponseEntity<String> response = restTemplate.exchange(
                url("/projetos/meus"),
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class);

            assertNaoAutorizado(response);
        }
    }

    @Nested
    @DisplayName("Story 1.2 — classificação de papel reflectida na sessão")
    class ClassificacaoDePapelNaSessao {

        @Test
        @DisplayName("Token de professor concede exatamente ROLE_PROFESSOR (nunca ROLE_ADMIN)")
        void professorNaoHerdarAdmin() {
            User professor = criarUsuario("Fernanda Profa", "fernanda.profa@cps.sp.gov.br", UserRole.PROFESSOR);

            String token = tokenDe(professor);
            ResponseEntity<String> response = post("/instituicoes",
                Map.of("codigoUnidade", "920", "nome", "Fatec Prof"), token);

            // Se o professor herdasse ADMIN, o catálogo seria alterado — violando NFR2.
            assertAcessoNegado(response);
        }

        @Test
        @DisplayName("Token de aluno em domínio não-CPS ainda é apenas ALUNO")
        void alunoDeDominioExternoNaoViraAdmin() {
            User usuario = criarUsuario("Gabriel Externo", "gabriel@exemplo.com", UserRole.ALUNO);

            ResponseEntity<String> response = post("/instituicoes",
                Map.of("codigoUnidade", "921", "nome", "Fatec Externa"), tokenDe(usuario));

            assertAcessoNegado(response);
        }
    }
}