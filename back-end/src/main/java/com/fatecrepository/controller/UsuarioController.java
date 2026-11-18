package com.fatecrepository.controller;

import com.fatecrepository.dto.response.PerfilPublicoUsuarioResponse;
import com.fatecrepository.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/usuarios")
@RequiredArgsConstructor
@Tag(name = "Usuários", description = "Endpoints públicos e de perfil de usuários")
public class UsuarioController {

    private final UsuarioService usuarioService;

    @GetMapping("/{id}/perfil")
    @Operation(summary = "Obter perfil público de um usuário",
        description = "Retorna os dados públicos de um aluno ou professor (nome, role, avatar, instituições vinculadas e projetos aprovados). Oculta e-mail conforme LGPD.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Perfil público retornado com sucesso"),
        @ApiResponse(responseCode = "404", description = "Usuário não encontrado"),
        @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    public ResponseEntity<PerfilPublicoUsuarioResponse> obterPerfilPublico(
        @Parameter(description = "ID do usuário") @PathVariable UUID id) {
        log.info("GET /usuarios/{}/perfil", id);
        return ResponseEntity.ok(usuarioService.obterPerfilPublico(id));
    }
}
