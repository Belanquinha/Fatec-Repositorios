package com.fatecrepository.service;

/**
 * Falha de entrega no provedor de e-mail, com o status HTTP quando houve resposta.
 * Carrega só o corpo/status devolvidos pelo provedor — nunca cabeçalhos (que
 * conteriam a chave da API) nem a chave em si.
 */
public class NotificacaoEnvioException extends RuntimeException {

    private final Integer statusHttp;

    public NotificacaoEnvioException(String mensagem, Integer statusHttp, Throwable causa) {
        super(mensagem, causa);
        this.statusHttp = statusHttp;
    }

    public Integer getStatusHttp() {
        return statusHttp;
    }
}
