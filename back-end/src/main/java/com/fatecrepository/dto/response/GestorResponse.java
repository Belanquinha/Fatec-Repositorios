package com.fatecrepository.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "GestorResponse", description = "Dados do gestor responsável pela instituição")
public class GestorResponse {
    @Schema(description = "ID do gestor")
    private UUID id;

    @Schema(description = "Nome completo do gestor")
    private String nome;

    @Schema(description = "Email do gestor")
    private String email;

    @Schema(description = "Telefone do gestor")
    private String telefone;

    @Schema(description = "Data de criação")
    private LocalDateTime criadoEm;

    @Schema(description = "Data da última atualização")
    private LocalDateTime atualizadoEm;
}
