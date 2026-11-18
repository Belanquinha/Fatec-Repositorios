package com.fatecrepository.service;

import com.fatecrepository.model.VerificationPurpose;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fallback exclusivo para perfis {@code dev}, {@code local} e {@code test} quando
 * {@code RESEND_API_KEY} está vazia: registra o código de 6 dígitos no log do back-end
 * para viabilizar desenvolvimento offline e testes automatizados E2E.
 */
@Slf4j
public class ConsoleDevAuthEmailSender implements AuthEmailSender {

    private final Map<String, String> ultimosCodigosPorChave = new ConcurrentHashMap<>();

    @Override
    public void enviarCodigoVerificacao(
        String emailDestino,
        String nomeDestinatario,
        String codigo,
        VerificationPurpose finalidade,
        int validadeMinutos
    ) {
        ultimosCodigosPorChave.put(chave(emailDestino, finalidade), codigo);
        log.warn("================================================================");
        log.warn("[MODO DEV — RESEND_API_KEY vazia] Código ({}) para {}: {} (válido por {} min)",
            finalidade, emailDestino, codigo, validadeMinutos);
        log.warn("================================================================");
    }

    public String obterUltimoCodigo(String emailDestino, VerificationPurpose finalidade) {
        if (emailDestino == null || finalidade == null) {
            return null;
        }
        return ultimosCodigosPorChave.get(chave(emailDestino.trim().toLowerCase(), finalidade));
    }

    public void limpar() {
        ultimosCodigosPorChave.clear();
    }

    private static String chave(String email, VerificationPurpose finalidade) {
        return email.trim().toLowerCase() + "::" + finalidade.name();
    }
}
