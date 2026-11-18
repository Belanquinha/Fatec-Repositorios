package com.fatecrepository.service;

import com.fatecrepository.model.VerificationPurpose;

/**
 * Envio transacional de códigos de verificação (cadastro e recuperação de senha).
 */
public interface AuthEmailSender {

    void enviarCodigoVerificacao(
        String emailDestino,
        String nomeDestinatario,
        String codigo,
        VerificationPurpose finalidade,
        int validadeMinutos
    );
}
