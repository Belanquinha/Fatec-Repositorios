package com.fatecrepository.e2e;

import com.fatecrepository.model.User;
import com.fatecrepository.model.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Contrato da política de autorização — a parte dela que é <em>padrão</em>, não exceção.
 *
 * <p>{@code SecurityConfig} lista as rotas públicas do produto (catálogo, documentação, upload
 * servido, Swagger) e fecha o resto com {@code anyRequest().authenticated()}. Isso é fail-closed de
 * propósito: com {@code permitAll()} no catch-all, um controller novo nascia público por omissão, e
 * a falha só apareceria em produção, como exposição de dados.
 *
 * <p>Esta suíte existe porque essa propriedade não é observável pelas rotas reais — hoje todas as
 * rotas da aplicação têm matcher explícito, então "o catch-all exige sessão" nunca é exercitado.
 * O teste abaixo marca uma rota que <em>não</em> tem matcher, que é exatamente a situação de um
 * controller recém-adicionado que ninguém lembra de registrar.
 */
@DisplayName("Política de acesso — rota sem matcher explícito exige sessão")
@Import(PoliticaDeAcessoE2ETest.RotaSemMatcher.class)
class PoliticaDeAcessoE2ETest extends ApiE2ETestSupport {

    @Test
    @DisplayName("Rota não listada em SecurityConfig devolve 401 sem token")
    void rotaSemMatcherExigeSessao() {
        ResponseEntity<String> response = get("/rota-sem-matcher-declaracao");

        assertNaoAutorizado(response);
    }

    @Test
    @DisplayName("Rota não listada em SecurityConfig devolve 200 com token válido")
    void rotaSemMatcherAceitaTokenValido() {
        User usuario = criarUsuario("Helena Rota", "helena.rota@aluno.cps.sp.gov.br", UserRole.ALUNO);

        ResponseEntity<String> response = get("/rota-sem-matcher-declaracao", tokenDe(usuario));

        assertThat(response.getStatusCode().value()).isEqualTo(200);
    }

    /**
     * Controller de teste, e não de produção: existe só para representar "alguém adicionou uma rota
     * e não a registrou". Vive em src/test e é importado só nesta suíte, então não aparece na
     * aplicação empacotada.
     */
    @RestController
    static class RotaSemMatcher {

        @GetMapping("/rota-sem-matcher-declaracao")
        String responding() {
            return "ok";
        }
    }
}