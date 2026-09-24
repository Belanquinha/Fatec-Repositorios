package com.fatecrepository.model;

public enum ProjetoEstado {
    AGUARDANDO_APROVACAO("AGUARDANDO_APROVACAO"),
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
