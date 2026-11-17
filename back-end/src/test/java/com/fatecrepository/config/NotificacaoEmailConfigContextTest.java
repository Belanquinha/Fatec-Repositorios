package com.fatecrepository.config;

import com.fatecrepository.service.NotificacaoEmailSender;
import com.fatecrepository.service.NotificacaoEmailSenderDesligado;
import com.fatecrepository.service.ResendEmailSender;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A trava é exercitada no contexto de verdade (não só no método) e as asserções
 * dizem o porquê da falha: não basta "o contexto falhou", é preciso que tenha
 * falhado pela trava do override.
 */
@DisplayName("ResendEmailConfig — contexto aborta sem profile de desenvolvimento")
class NotificacaoEmailConfigContextTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
        .withUserConfiguration(TestConfig.class)
        .withPropertyValues("app.resend.api-key=re_key");

    @Test
    @DisplayName("prod + override: contexto falha ao subir pela trava do override")
    void prodComOverrideFalha() {
        runner.withPropertyValues(
                "app.resend.override-destinatario=dev@meu-email.com",
                "spring.profiles.active=prod")
            .run(context -> {
                assertThat(context).hasFailed();
                assertThat(context).getFailure()
                    .hasRootCauseInstanceOf(IllegalStateException.class);
                assertThat(context).getFailure()
                    .hasStackTraceContaining("override-destinatario");
            });
    }

    @Test
    @DisplayName("sem profile + override: contexto falha (fail-closed)")
    void semProfileComOverrideFalha() {
        runner.withPropertyValues("app.resend.override-destinatario=dev@meu-email.com")
            .run(context -> {
                assertThat(context).hasFailed();
                assertThat(context).getFailure()
                    .hasRootCauseInstanceOf(IllegalStateException.class);
                assertThat(context).getFailure()
                    .hasStackTraceContaining("override-destinatario");
            });
    }

    @Test
    @DisplayName("dev + override: contexto sobe e o sender é o Resend")
    void devComOverrideSobe() {
        runner.withPropertyValues(
                "app.resend.override-destinatario=dev@meu-email.com",
                "spring.profiles.active=dev")
            .run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context).getBean(NotificacaoEmailSender.class)
                    .isInstanceOf(ResendEmailSender.class);
            });
    }

    @Test
    @DisplayName("sem override em prod: contexto sobe e o sender é o Resend (key configurada)")
    void prodSemOverrideSobe() {
        runner.withPropertyValues("spring.profiles.active=prod")
            .run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context).getBean(NotificacaoEmailSender.class)
                    .isInstanceOf(ResendEmailSender.class);
            });
    }

    @Test
    @DisplayName("sem key: o sender é o no-op, não o Resend")
    void semKeyUsaSenderDesligado() {
        runner.withPropertyValues("app.resend.api-key=")
            .run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context).getBean(NotificacaoEmailSender.class)
                    .isInstanceOf(NotificacaoEmailSenderDesligado.class);
            });
    }

    /** Apenas as classes da notificação — sem JPA/datasource nesta verificação. */
    @org.springframework.context.annotation.Configuration
    @org.springframework.boot.context.properties.EnableConfigurationProperties(ResendProperties.class)
    @org.springframework.context.annotation.Import({ResendEmailConfig.class, NotificacaoEmailConfig.class})
    static class TestConfig {
    }
}
