package com.fatecrepository.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Aviso na subida quando o login de desenvolvimento está ligado.
 *
 * <p>O endpoint de dev-login emite um JWT válido para qualquer e-mail, inclusive o admin. Um
 * interruptedor em um arquivo de configuração é fácil de ligar e esquecer de desligar, e o síntoma
 * desse esquecimento — alguém entrando como admin — não aparece em nenhum log de acesso. Daí este
 * banner: ele não impede nada, mas torna o estado da aplicação visível em um `docker compose up`.
 */
@Slf4j
@Component
@Profile("dev")
@ConditionalOnProperty(name = "app.security.dev-auth.enabled", havingValue = "true")
public class DevAuthStartupWarning {

    @EventListener(ApplicationReadyEvent.class)
    public void avisar() {
        log.warn("""

                ******************************************************************************
                *  LOGIN DE DESENVOLVIMENTO ATIVO (/auth/dev-login)
                *
                *  Qualquer um que alcance esta API recebe um JWT válido para o e-mail que
                *  informar, inclusive a conta de administrador. Não exponha esta porta.
                *
                *  Para desligar: remova SPRING_PROFILES_ACTIVE=dev e/ou
                *  DEV_AUTH_ENABLED=true do ambiente.
                ******************************************************************************
                """);
    }
}