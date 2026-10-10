package com.fatecrepository.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RejeitarProjetoRequest {

    @NotBlank(message = "O motivo da rejeição é obrigatório")
    @Size(max = 2000, message = "O motivo da rejeição deve ter no máximo 2000 caracteres")
    private String motivo;
}
