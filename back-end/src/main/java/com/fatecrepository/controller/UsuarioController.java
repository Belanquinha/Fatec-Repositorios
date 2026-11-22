package com.fatecrepository.controller;

import com.fatecrepository.dto.response.MensagemResponse;
import com.fatecrepository.dto.response.PerfilPublicoUsuarioResponse;
import com.fatecrepository.security.CustomUserDetails;
import com.fatecrepository.service.UsuarioExclusaoService;
import com.fatecrepository.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
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
    private final UsuarioExclusaoService usuarioExclusaoService;

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

    @DeleteMapping("/me")
    @Operation(summary = "Excluir a própria conta",
        description = "Anonimiza a conta (nome/e-mail/senha/foto), mantém os projetos APROVADOS com autoria anônima, "
            + "exclui os não-aprovados e devolve ao autor os pendentes em que o usuário era o professor responsável. "
            + "Tokens antigos deixam de funcionar imediatamente.")
    @SecurityRequirement(name = "Bearer JWT")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Conta removida"),
        @ApiResponse(responseCode = "401", description = "Não autenticado"),
        @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
    })
    public ResponseEntity<MensagemResponse> excluirPropriaConta(
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        if (userDetails == null) {
            throw new com.fatecrepository.exception.UnauthorizedException("Usuário não autenticado");
        }
        log.info("DELETE /usuarios/me pelo usuário: {}", userDetails.getId());
        return ResponseEntity.ok(usuarioExclusaoService.excluirConta(userDetails.getId()));
    }
}
