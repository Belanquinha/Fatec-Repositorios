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
@Schema(name = "InstituicaoResponse", description = "Dados de uma instituição do catálogo oficial")
public class InstituicaoResponse {
    @Schema(description = "ID da instituição")
    private UUID id;

    @Schema(description = "Código oficial da unidade")
    private String codigoUnidade;

    @Schema(description = "Nome da instituição")
    private String nome;

    @Schema(description = "Endereço da instituição")
    private String endereco;

    @Schema(description = "Cidade da instituição")
    private String cidade;

    @Schema(description = "Estado da instituição")
    private String estado;

    @Schema(description = "Região administrativa")
    private String regiaoAdministrativa;

    @Schema(description = "CNPJ da instituição")
    private String cnpj;

    @Schema(description = "Telefones da instituição")
    private String telefone;

    @Schema(description = "Site oficial")
    private String site;

    @Schema(description = "Link do logotipo")
    private String linkLogo;

    @Schema(description = "Se a instituição está ativa no catálogo")
    private boolean ativo;

    @Schema(description = "Data de criação")
    private LocalDateTime criadoEm;

    @Schema(description = "Data da última atualização")
    private LocalDateTime atualizadoEm;
}