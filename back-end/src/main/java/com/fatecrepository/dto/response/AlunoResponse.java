package com.fatecrepository.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlunoResponse {
    private UUID id;
    private String matricula;
    private String telefone;
    private UsuarioResponse usuario;
    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;
}
