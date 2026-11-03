package com.fatecrepository.model;

public enum ProjetoEstado {
    SUBMETIDO("SUBMETIDO"),
    AGUARDANDO_APROVACAO("AGUARDANDO_APROVACAO"),
    ALTERACOES_SOLICITADAS("ALTERACOES_SOLICITADAS"),
    REENVIADO("REENVIADO"),
    APROVADO("APROVADO"),
    REJEITADO("REJEITADO");

    private final String value;

    ProjetoEstado(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static ProjetoEstado fromValue(String value) {
        for (ProjetoEstado estado : ProjetoEstado.values()) {
            if (estado.value.equalsIgnoreCase(value)) {
                return estado;
            }
        }
        throw new IllegalArgumentException("Estado de projeto desconhecido: " + value);
    }
}
