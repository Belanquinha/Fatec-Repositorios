package com.fatecrepository.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/** Item de {@code GET /projetos/removidos}: sem PII, com a contagem regressiva. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjetoRemovidoResponse {
    private UUID id;
    private String titulo;
    private String estadoAnterior;
    private LocalDateTime removidoEm;
    private String motivoRemocaoGenerico;
    private long diasParaExpirar;
}
