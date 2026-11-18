package com.fatecrepository.dto.response;

import com.fatecrepository.model.UserRole;
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
@Schema(name = "PerfilPublicoUsuarioResponse", description = "Perfil público seguro de um usuário (aluno ou professor), omitindo dados sensíveis como e-mail (LGPD)")
public class PerfilPublicoUsuarioResponse {
    @Schema(description = "ID do usuário")
    private UUID id;

    @Schema(description = "Nome de exibição do usuário")
    private String nome;

    @Schema(description = "Papel/função do usuário (ALUNO, PROFESSOR, ADMIN)")
    private UserRole role;

    @Schema(description = "URL do avatar do usuário")
    private String fotoUrl;

    @Schema(description = "Instituições associadas (para professores)")
    private List<InstituicaoResponse> instituicoes;

    @Schema(description = "Lista de projetos aprovados em que o usuário é autor ou professor avaliador")
    private List<ProjetoResponse> projetos;
}
