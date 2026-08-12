package com.fatecrepository.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InstituicaoResponse {
    private UUID id;
    private String nome;
    private String cnpj;
    private String email;
    private String telefone;
    private String endereco;
    private String cidade;
    private String estado;
    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;
}
