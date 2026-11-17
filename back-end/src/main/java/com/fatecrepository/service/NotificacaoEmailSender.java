package com.fatecrepository.service;

/**
 * Contrato da entrega de notificação de projeto submetido.
 *
 * <p>Detrás dele fica o Resend ({@code ResendEmailSender}); sem key configurada
 * entra {@code NotificacaoEmailSenderDesligado} (no-op). Nos testes o bean é
 * substituído por spy/mock, então nenhuma suíte precisa de rede nem de credencial.
 */
public interface NotificacaoEmailSender {

    /**
     * Envia uma notificação. Falhas de provedor (HTTP 4xx/5xx, timeout, DNS)
     * devem chegar como exceção; quem chama decide se isso desfaz a operação —
     * no caso da submissão, não.
     */
    void enviar(ProjetoSubmetidoNotificacao notificacao);

    /** Se a integração está configurada (key presente). Desligada = não tenta enviar. */
    boolean habilitado();
}
