package com.fatecrepository.model;

/** Categoria genérica do motivo de remoção, nunca um texto livre com PII. */
public enum MotivoRemocao {
    PEDIDO_AUTOR,
    DADOS_PESSOAIS,
    ERRO_AUTORIA,
    OUTRO;

    public static boolean contem(String valor) {
        if (valor == null) {
            return false;
        }
        for (MotivoRemocao motivo : values()) {
            if (motivo.name().equalsIgnoreCase(valor.trim())) {
                return true;
            }
        }
        return false;
    }
}
