package com.fatecrepository.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "VinculoInstituicoesRequest", description = "Conjunto completo de vínculos do professor (substituição total via PUT)")
public class VinculoInstituicoesRequest {

    @NotNull(message = "instituicaoIds é obrigatório (use [] para limpar os vínculos)")
    @Schema(description = "IDs das instituições vinculadas (0 a 4, sem duplicados, sem nulos)", example = "[\"3fa85f64-5717-4562-b3fc-2c963f66afa6\"]")
    private List<@jakarta.validation.constraints.NotNull(message = "instituicaoIds contém id nulo") UUID> instituicaoIds;
}
