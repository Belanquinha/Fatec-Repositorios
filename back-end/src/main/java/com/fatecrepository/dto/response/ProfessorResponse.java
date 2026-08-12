package com.fatecrepository.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProfessorResponse {
    private UUID id;
    private String telefone;
    private String areaEnsino;
    private UsuarioResponse usuario;
    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;
}
