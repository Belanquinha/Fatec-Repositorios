package com.fatecrepository.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Corpo do {@code PATCH /projetos/{id}/devolver} (SPEC-exclusao-projetos §4.3).
 *
 * <p>Diferente da remoção de projeto, aqui o motivo é texto livre e obrigatório:
 * é a justificativa que o autor vai ler para saber o que ajustar.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DevolverProjetoRequest {

    @NotBlank(message = "O motivo da devolução é obrigatório")
    @Size(max = 2000, message = "O motivo da devolução deve ter no máximo 2000 caracteres")
    private String motivo;
}
