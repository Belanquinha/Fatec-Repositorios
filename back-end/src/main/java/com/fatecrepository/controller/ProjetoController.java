package com.fatecrepository.controller;

import com.fatecrepository.dto.request.ProjetoCreateRequest;
import com.fatecrepository.dto.request.RejeitarProjetoRequest;
import com.fatecrepository.dto.response.ProjetoResponse;
import com.fatecrepository.exception.UnauthorizedException;
import com.fatecrepository.security.CustomUserDetails;
import com.fatecrepository.service.ProjetoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/projetos")
@RequiredArgsConstructor
@Tag(name = "Projetos", description = "Endpoints para submissão e consulta de projetos acadêmicos")
public class ProjetoController {

    private final ProjetoService projetoService;

    @PostMapping
    @Operation(summary = "Criar e submeter projeto para avaliação acadêmica", description = "Submete um novo projeto acadêmico que nasce no estado AGUARDANDO_APROVACAO")
    @SecurityRequirement(name = "Bearer JWT")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Projeto submetido com sucesso"),
        @ApiResponse(responseCode = "400", description = "Dados da requisição inválidos"),
        @ApiResponse(responseCode = "401", description = "Não autenticado"),
        @ApiResponse(responseCode = "404", description = "Instituição ou usuário não encontrado")
    })
    public ResponseEntity<ProjetoResponse> criar(
        @Valid @RequestBody ProjetoCreateRequest request,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        if (userDetails == null) {
            throw new UnauthorizedException("Usuário não autenticado");
        }
        log.info("POST /projetos: submetendo projeto '{}' pelo usuário: {}", request.getTitulo(), userDetails.getId());
        ProjetoResponse response = projetoService.criar(request, userDetails.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/publicos")
    @Operation(summary = "Listar projetos públicos aprovados", description = "Retorna todos os projetos aprovados para a vitrine pública")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lista de projetos públicos retornada")
    })
    public ResponseEntity<List<ProjetoResponse>> listarPublicos() {
        log.info("GET /projetos/publicos");
        List<ProjetoResponse> response = projetoService.listarPublicos();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/pendentes")
    @PreAuthorize("hasRole('PROFESSOR')")
    @Operation(summary = "Listar projetos pendentes para o professor logado",
        description = "Retorna os projetos em AGUARDANDO_APROVACAO onde o professor autenticado é o responsável indicado "
            + "e está vinculado à instituição do projeto. Usado na tela do professor (/perfil) para aprovar o fluxo de submissão.")
    @SecurityRequirement(name = "Bearer JWT")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lista de pendentes retornada (vazia quando sem vínculo ou sem indicação)"),
        @ApiResponse(responseCode = "401", description = "Não autenticado"),
        @ApiResponse(responseCode = "403", description = "Papel diferente de PROFESSOR")
    })
    public ResponseEntity<List<ProjetoResponse>> listarPendentes(
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        if (userDetails == null) {
            throw new UnauthorizedException("Usuário não autenticado");
        }
        log.info("GET /projetos/pendentes para o professor: {}", userDetails.getId());
        return ResponseEntity.ok(projetoService.listarPendentesParaProfessor(userDetails.getId()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obter projeto por ID", description = "Retorna os detalhes de um projeto específico")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Projeto encontrado"),
        @ApiResponse(responseCode = "404", description = "Projeto não encontrado")
    })
    public ResponseEntity<ProjetoResponse> obterPorId(@Parameter(description = "ID do projeto") @PathVariable UUID id) {
        log.info("GET /projetos/{}", id);
        ProjetoResponse response = projetoService.obterPorId(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/meus")
    @Operation(summary = "Listar projetos do usuário logado", description = "Retorna todos os projetos submetidos pelo aluno autenticado")
    @SecurityRequirement(name = "Bearer JWT")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lista de projetos do usuário retornada"),
        @ApiResponse(responseCode = "401", description = "Não autenticado")
    })
    public ResponseEntity<List<ProjetoResponse>> obterMeusProjetos(@AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            throw new UnauthorizedException("Usuário não autenticado");
        }
        log.info("GET /projetos/meus para o usuário: {}", userDetails.getId());
        List<ProjetoResponse> response = projetoService.obterMeusProjetos(userDetails.getId());
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/aprovar")
    @PreAuthorize("hasRole('PROFESSOR')")
    @Operation(summary = "Aprovar projeto pendente",
        description = "Aprova um projeto em AGUARDANDO_APROVACAO. Somente o professor responsável indicado na submissão "
            + "e vinculado à instituição do projeto pode aprovar (403 caso contrário).")
    @SecurityRequirement(name = "Bearer JWT")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Projeto aprovado"),
        @ApiResponse(responseCode = "401", description = "Não autenticado"),
        @ApiResponse(responseCode = "403", description = "Professor não é o responsável ou não é vinculado à instituição"),
        @ApiResponse(responseCode = "404", description = "Projeto não encontrado"),
        @ApiResponse(responseCode = "409", description = "Projeto já avaliado")
    })
    public ResponseEntity<ProjetoResponse> aprovar(
        @Parameter(description = "ID do projeto") @PathVariable UUID id,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        if (userDetails == null) {
            throw new UnauthorizedException("Usuário não autenticado");
        }
        log.info("PATCH /projetos/{}/aprovar pelo professor: {}", id, userDetails.getId());
        return ResponseEntity.ok(projetoService.aprovar(id, userDetails.getId()));
    }

    @PatchMapping("/{id}/rejeitar")
    @PreAuthorize("hasRole('PROFESSOR')")
    @Operation(summary = "Rejeitar projeto pendente",
        description = "Rejeita um projeto em AGUARDANDO_APROVACAO com motivo obrigatório. Mesma regra de permissão da aprovação.")
    @SecurityRequirement(name = "Bearer JWT")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Projeto rejeitado"),
        @ApiResponse(responseCode = "400", description = "Motivo ausente"),
        @ApiResponse(responseCode = "401", description = "Não autenticado"),
        @ApiResponse(responseCode = "403", description = "Professor não é o responsável ou não é vinculado à instituição"),
        @ApiResponse(responseCode = "404", description = "Projeto não encontrado"),
        @ApiResponse(responseCode = "409", description = "Projeto já avaliado")
    })
    public ResponseEntity<ProjetoResponse> rejeitar(
        @Parameter(description = "ID do projeto") @PathVariable UUID id,
        @Valid @RequestBody RejeitarProjetoRequest request,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        if (userDetails == null) {
            throw new UnauthorizedException("Usuário não autenticado");
        }
        log.info("PATCH /projetos/{}/rejeitar pelo professor: {}", id, userDetails.getId());
        return ResponseEntity.ok(projetoService.rejeitar(id, userDetails.getId(), request.getMotivo()));
    }
}
