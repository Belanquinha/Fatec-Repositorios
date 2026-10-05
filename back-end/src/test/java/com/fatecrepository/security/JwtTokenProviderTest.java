package com.fatecrepository.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * O segredo que assina o JWT é a única coisa que separa "sessão legítima" de "sessão forjada": com
 * HS256, conhecer o segredo é saber assinar token de admin. O valor padrão anterior foi versionado
 * no repositório por meses, o que tornava qualquer deploy que o usasse forjável.
 *
 * <p>Estes testes existem para que a validação não seja removida como "redundante": ela não é. O
 * {@code Algorithm.HMAC256} do java-jwt aceita qualquer string, inclusive uma letra, sem reclamar —
 * e um segredo fraco não dá erro, dá uma chave que se quebra por força bruta. Falhar na subida do
 * contexto é a diferença entre um deploy quebrado e uma sessão de admin forjada.
 */
@DisplayName("Validação do segredo que assina o JWT")
class JwtTokenProviderTest {

    private static final String SEGREDO_VALIDO = "segredo-de-teste-com-mais-de-32-bytes";

    @Test
    @DisplayName("Segredo com 32 bytes ou mais é aceito")
    void segredoLongoEhAceito() {
        JwtTokenProvider provider = comSegredo(SEGREDO_VALIDO);

        provider.init();

        assertThat(gerarTokenComEmail(provider)).isNotBlank();
    }

    @Test
    @DisplayName("Segredo de 31 bytes é recusado — o limite é 32")
    void segredoNoLimiteEhRecusado() {
        JwtTokenProvider provider = comSegredo("a".repeat(31));

        assertThatThrownBy(provider::init)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("31")
                .hasMessageContaining("32");
    }

    @Test
    @DisplayName("Segredo de um caractere é recusado")
    void segredoMinusculoEhRecusado() {
        JwtTokenProvider provider = comSegredo("a");

        assertThatThrownBy(provider::init)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("app.security.jwt.secret");
    }

    @Test
    @DisplayName("Segredo vazio ou só com espaços é recusado")
    void segredoVazioEhRecusado() {
        assertThatThrownBy(() -> comSegredo("   ").init())
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Segredo ausente (null) é recusado, e não NullPointerException")
    void segredoNuloEhRecusado() {
        assertThatThrownBy(() -> comSegredo(null).init())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ausente");
    }

    @Test
    @DisplayName("A mensagem de recusa diz como corrigir")
    void mensagemIndicaComoGerar() {
        assertThatThrownBy(() -> comSegredo("curto").init())
                .hasMessageContaining("openssl rand");
    }

    private JwtTokenProvider comSegredo(String segredo) {
        JwtTokenProvider provider = new JwtTokenProvider();
        ReflectionTestUtils.setField(provider, "jwtSecret", segredo);
        ReflectionTestUtils.setField(provider, "jwtIssuer", "fatec-repository-api");
        ReflectionTestUtils.setField(provider, "tokenExpirationHours", 24L);
        return provider;
    }

    private String gerarTokenComEmail(JwtTokenProvider provider) {
        com.fatecrepository.model.User user = new com.fatecrepository.model.User();
        java.util.UUID id = java.util.UUID.randomUUID();
        ReflectionTestUtils.setField(user, "id", id);
        ReflectionTestUtils.setField(user, "email", "aluno@aluno.cps.sp.gov.br");
        ReflectionTestUtils.setField(user, "nome", "Aluno Teste");
        ReflectionTestUtils.setField(user, "role", com.fatecrepository.model.UserRole.ALUNO);
        return provider.generateToken(user);
    }
}
