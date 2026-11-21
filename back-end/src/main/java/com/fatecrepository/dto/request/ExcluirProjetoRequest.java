package com.fatecrepository.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Corpo opcional do {@code DELETE /projetos/{id}}. A remoção em si não depende do
 * motivo; o valor só é registrado como categoria genérica, nunca como texto livre.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExcluirProjetoRequest {

    @NotNull(message = "Motivo da remoção inválido")
    @Size(max = 40, message = "Motivo da remoção inválido")
    private String motivoRemocao;
}
