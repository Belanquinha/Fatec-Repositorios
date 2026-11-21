package com.fatecrepository.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/** Resposta de {@code DELETE /projetos/{id}}: confirma a operação sem devolver PII. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExclusaoProjetoResponse {
    private UUID id;
    private String titulo;
    private String estado;
    private LocalDateTime removidoEm;
}
