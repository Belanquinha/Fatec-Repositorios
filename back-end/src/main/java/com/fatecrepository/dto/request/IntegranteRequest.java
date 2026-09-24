package com.fatecrepository.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IntegranteRequest {

    @NotBlank(message = "Nome do integrante é obrigatório")
    private String nome;

    private String linkLinkedin;
}
