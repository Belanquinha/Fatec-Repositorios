package com.fatecrepository.config;

import com.fatecrepository.service.NotificacaoEmailSender;
import com.fatecrepository.service.NotificacaoEmailSenderDesligado;
import com.fatecrepository.service.ResendEmailSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * Escolha do {@link NotificacaoEmailSender}: com key configurada, o Resend; sem
 * key, o no-op que só registra o motivo e não faz chamada alguma.
 *
 * <p>Dois beans do mesmo tipo nunca coexistem aqui — a decisão é por key presente,
 * não por ordem de registro — assim a injeção em {@code NotificacaoProjetoService}
 * é sempre inequívoca.
 *
 * <p>O {@link RestClient.Builder} é declarado aqui porque o Boot 4 não mais
 * auto-configura um (não há starter webclient no classpath). O client usa a factory
 * do JDK com connect/read timeout: sem eles, um Resend lento ou fora do ar prenderia
 * a thread do pool indefinidamente e, com o pool pequeno, as notificações seguintes
 * seriam descartadas.
 */
@Slf4j
@Configuration
public class NotificacaoEmailConfig {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(10);

    @Bean
    public RestClient.Builder notificacaoRestClientBuilder() {
        HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(CONNECT_TIMEOUT)
            .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(READ_TIMEOUT);
        return RestClient.builder().requestFactory(requestFactory);
    }

    @Bean
    public NotificacaoEmailSender resendEmailSender(
        ResendProperties props,
        RestClient.Builder restClientBuilder) {
        if (!props.habilitado()) {
            log.warn("app.resend.api-key ausente: notificação de submissão desligada "
                + "(submissões seguem normais, sem alerta ao professor).");
            return new NotificacaoEmailSenderDesligado();
        }
        return new ResendEmailSender(props, restClientBuilder.build());
    }
}
