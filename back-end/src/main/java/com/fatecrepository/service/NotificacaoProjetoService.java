package com.fatecrepository.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Notificação "projeto submetido" ao professor responsável.
 *
 * <p>Desenho best-effort de primeira versão: uma única tentativa, sem retry, sem
 * fila durável. A falha é logada e engolida — quem chama (submissão) já commitou
 * o projeto, então um problema de e-mail nunca pode desfazer ou pendurar a
 * submissão. Limitação conhecida: se o processo cair entre o commit e o envio, o
 * alerta se perde; não há rastro em banco nem reenvio nesta versão.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificacaoProjetoService {

    private final NotificacaoEmailSender sender;

    /**
     * Disparo assíncrono (pós-commit). O método é void de propósito: exceção aqui
     * não tem para onde subir que mudasse o resultado da submissão.
     */
    @Async("notificacaoExecutor")
    public void notificarProjetoSubmetido(ProjetoSubmetidoNotificacao notificacao) {
        if (notificacao == null) {
            return;
        }
        if (!sender.habilitado()) {
            log.warn("Notificação de projeto desligada (app.resend.api-key ausente): projetoId={} segue sem alerta",
                notificacao.projetoId());
            return;
        }
        try {
            sender.enviar(notificacao);
        } catch (Exception e) {
            // Sem retry e sem rethrow: o projeto está commitado; o registro do
            // problema fica no log, com id do projeto e detalhe do provedor.
            Integer statusHttp = e instanceof NotificacaoEnvioException n ? n.getStatusHttp() : null;
            log.error("Falha (sem retry) ao notificar o professor sobre o projeto {}: "
                    + "statusHttp={}, mensagem={}",
                notificacao.projetoId(), statusHttp, e.getMessage(), e);
        }
    }
}
