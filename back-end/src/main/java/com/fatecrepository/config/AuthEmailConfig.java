package com.fatecrepository.config;

import com.fatecrepository.exception.ServiceUnavailableException;
import com.fatecrepository.service.AuthEmailSender;
import com.fatecrepository.service.ConsoleDevAuthEmailSender;
import com.fatecrepository.service.ResendAuthEmailSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.web.client.RestClient;

/**
 * Configura o {@link AuthEmailSender} para envio dos códigos de verificação:
 * <ul>
 *   <li>Com {@code RESEND_API_KEY} preenchida (em {@code dev} ou {@code prod}): envia via Resend.</li>
 *   <li>Sem key em {@code dev}/{@code local}/{@code test}: usa {@link ConsoleDevAuthEmailSender}.</li>
 *   <li>Sem key em produção (ou sem perfil de dev/teste): falha com 503 ao tentar enviar código.</li>
 * </ul>
 */
@Slf4j
@Configuration
public class AuthEmailConfig {

    @Bean
    public AuthEmailSender authEmailSender(
        ResendProperties props,
        RestClient.Builder notificacaoRestClientBuilder,
        Environment environment
    ) {
        if (props.habilitado()) {
            log.info("AuthEmailSender ativo via Resend (remetente={})", props.getRemetente());
            return new ResendAuthEmailSender(props, notificacaoRestClientBuilder.build());
        }

        if (aceitaFallbackConsole(environment)) {
            log.warn("RESEND_API_KEY ausente em ambiente de desenvolvimento/teste: "
                + "códigos de verificação serão exibidos no log do back-end.");
            return new ConsoleDevAuthEmailSender();
        }

        log.error("RESEND_API_KEY ausente fora de dev/local/test: envio de códigos de verificação indisponível.");
        return (emailDestino, nomeDestinatario, codigo, finalidade, validadeMinutos) -> {
            throw new ServiceUnavailableException(
                "Serviço de envio de e-mail não configurado no servidor."
            );
        };
    }

    private static boolean aceitaFallbackConsole(Environment env) {
        for (String perfil : env.getActiveProfiles()) {
            if (perfil == null) {
                continue;
            }
            String normalizado = perfil.toLowerCase();
            if (normalizado.startsWith("dev")
                || normalizado.startsWith("local")
                || normalizado.startsWith("test")) {
                return true;
            }
        }
        return false;
    }
}
