package com.fatecrepository.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioResponse {
    private UUID id;
    private String nome;
    private String email;
    private String role;
    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;
}
