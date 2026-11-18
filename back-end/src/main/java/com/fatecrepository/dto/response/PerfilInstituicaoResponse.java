package com.fatecrepository.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "PerfilInstituicaoResponse", description = "Perfil público completo de uma instituição, incluindo cursos oferecidos e projetos aprovados")
public class PerfilInstituicaoResponse {
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

    @Schema(description = "Telefones da instituição")
    private String telefone;

    @Schema(description = "Site oficial")
    private String site;

    @Schema(description = "Link do logotipo")
    private String linkLogo;

    @Schema(description = "Se a instituição está ativa no catálogo")
    private boolean ativo;

    @Schema(description = "Cursos ativos oferecidos por esta instituição")
    private List<CursoResponse> cursos;

    @Schema(description = "Projetos aprovados vinculados a esta instituição")
    private List<ProjetoResponse> projetos;
}
