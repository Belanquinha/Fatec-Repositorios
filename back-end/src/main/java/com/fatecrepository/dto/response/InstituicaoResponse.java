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
@Schema(name = "InstituicaoResponse", description = "Dados de uma instituição com o gestor responsável")
public class InstituicaoResponse {
    @Schema(description = "ID da instituição")
    private UUID id;

    @Schema(description = "Nome da instituição")
    private String nome;

    @Schema(description = "CNPJ da instituição")
    private String cnpj;

    @Schema(description = "Endereço da instituição")
    private String endereco;

    @Schema(description = "Cidade da instituição")
    private String cidade;

    @Schema(description = "Estado da instituição")
    private String estado;

    @Schema(description = "Dados do gestor responsável pela instituição")
    private GestorResponse gestor;

    @Schema(description = "Data de criação")
    private LocalDateTime criadoEm;

    @Schema(description = "Data da última atualização")
    private LocalDateTime atualizadoEm;
}
