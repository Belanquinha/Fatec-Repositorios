package com.fatecrepository.service;

import lombok.extern.slf4j.Slf4j;

/**
 * Substituto da integração quando {@code app.resend.api-key} está vazia: registra
 * o motivo e não faz chamada alguma. Assim dev local e os testes E2E sobem sem
 * credencial e sem tentar rede — a submissão segue normal, só sem alerta.
 */
@Slf4j
public class NotificacaoEmailSenderDesligado implements NotificacaoEmailSender {

    public NotificacaoEmailSenderDesligado() {
        log.warn("Notificação de e-mail desligada: app.resend.api-key não configurada. "
            + "Submissões seguem normais, mas o professor responsável não recebe alerta.");
    }

    @Override
    public void enviar(ProjetoSubmetidoNotificacao notificacao) {
        // Nunca chamado: o serviço consulta habilitado() antes.
    }

    @Override
    public boolean habilitado() {
        return false;
    }
}
