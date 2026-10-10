package com.fatecrepository.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RejeitarProjetoRequest {

    @NotBlank(message = "O motivo da rejeição é obrigatório")
    private String motivo;
}
