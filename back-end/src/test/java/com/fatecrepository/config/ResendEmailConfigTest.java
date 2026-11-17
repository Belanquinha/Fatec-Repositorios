package com.fatecrepository.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Trava do override de destinatário: fail-closed.
 *
 * <p>O override só é aceito com um profile de desenvolvimento ({@code dev*} ou
 * {@code local*}) ativo. Sem profile, com {@code prod} ou com qualquer outro nome,
 * o contexto aborta — "ausência de profile" não pode significar "pode redirecionar
 * todas as notificações para a caixa do desenvolvedor".
 *
 * <p>Com a integração habilitada, o remetente também é validado: {@code from} vazio
 * seria recusado pelo provedor em todas as notificações.
 */
@DisplayName("ResendEmailConfig — fail-closed do override e remetente")
class ResendEmailConfigTest {

    private ResendProperties props(String override, String remetente) {
        ResendProperties p = new ResendProperties();
        p.setApiKey("re_key");
        p.setOverrideDestinatario(override);
        p.setRemetente(remetente);
        return p;
    }

    private ResendEmailConfig config(ResendProperties props, String... profiles) {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles(profiles);
        return new ResendEmailConfig(props, env);
    }

    @Test
    @DisplayName("Override preenchido com profile prod aborta a subida do contexto")
    void prodComOverrideAborta() {
        ResendEmailConfig config = config(props("dev@meu-email.com", "naoresponda@cps.sp.gov.br"), "prod");

        assertThatThrownBy(config::afterPropertiesSet)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("override-destinatario");
    }

    @Test
    @DisplayName("Override preenchido sem profile ativo aborta (fail-closed)")
    void semProfileComOverrideAborta() {
        ResendEmailConfig config = config(props("dev@meu-email.com", "naoresponda@cps.sp.gov.br"));

        assertThatThrownBy(config::afterPropertiesSet)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("override-destinatario");
    }

    @Test
    @DisplayName("Override preenchido com profile dev é permitido")
    void devComOverridePermite() {
        ResendEmailConfig config = config(props("dev@meu-email.com", "naoresponda@cps.sp.gov.br"), "dev");

        assertThatCode(config::afterPropertiesSet).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Override preenchido com profile local é permitido")
    void localComOverridePermite() {
        ResendEmailConfig config = config(props("dev@meu-email.com", "naoresponda@cps.sp.gov.br"), "local");

        assertThatCode(config::afterPropertiesSet).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Override vazio com profile prod sobe normal (nada a bloquear)")
    void prodSemOverridePermite() {
        ResendEmailConfig config = config(props("  ", "naoresponda@cps.sp.gov.br"), "prod");

        assertThatCode(config::afterPropertiesSet).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Sem profile e sem override o contexto sobe (submissão segue sem alerta)")
    void semProfileSemOverridePermite() {
        ResendEmailConfig config = config(props("", "naoresponda@cps.sp.gov.br"));

        assertThatCode(config::afterPropertiesSet).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Remetente vazio com a integração ligada aborta (from vazio é recusado)")
    void remetenteVazioComKeyAborta() {
        ResendEmailConfig config = config(props("", "  "), "dev");

        assertThatThrownBy(config::afterPropertiesSet)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("remetente");
    }

    @Test
    @DisplayName("Remetente vazio sem key não aborta (integração desligada nem envia)")
    void remetenteVazioSemKeyPermite() {
        ResendProperties p = props("", "  ");
        p.setApiKey("");
        ResendEmailConfig config = config(p, "dev");

        assertThatCode(config::afterPropertiesSet).doesNotThrowAnyException();
    }
}
