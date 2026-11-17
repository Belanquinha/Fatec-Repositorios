package com.fatecrepository.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuração da integração de e-mail (Resend), sempre no back-end.
 *
 * <p>A chave NUNCA vai para o front-end: o Angular não envia e-mail. A key é
 * opcional de propósito — sem ela o {@code NotificacaoEmailSender} correspondente
 * se autodesliga ({@code enabled=false}) e a submissão segue normal, só sem alerta.
 * Assim o stack sobe em dev e nos testes E2E sem credencial.
 *
 * <p>{@code overrideDestinatario} só existe para desenvolvimento: redireciona a
 * entrega para a caixa de entrada do desenvolvedor, porque a conta de teste do
 * professor ({@code professor.teste@cps.sp.gov.br}) não tem caixa acessível. Em
 * produção o bean aborta a subida do contexto se este campo estiver preenchido
 * (ver {@code ResendEmailConfig}).
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.resend")
public class ResendProperties {

    /** Chave da API do Resend. Vazia = integração desligada. */
    private String apiKey = "";

    /**
     * Remetente verificado no Resend ("from"). Precisa ser domínio verificado;
     * caso contrário o Resend responde 403 e a notificação falha (best-effort).
     */
    private String remetente = "naoresponda@cps.sp.gov.br";

    /** URL base da plataforma, usada no corpo como orientação para abrir o sistema. */
    private String urlBaseFrontEnd = "http://localhost:4200";

    /**
     * Destinatário de desenvolvimento. Quando preenchido, a entrega vai para cá
     * em vez do e-mail do professor responsável. Nunca aceito do request.
     */
    private String overrideDestinatario = "";

    public boolean habilitado() {
        return apiKey != null && !apiKey.isBlank();
    }
}
