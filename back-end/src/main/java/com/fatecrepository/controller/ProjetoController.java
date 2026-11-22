package com.fatecrepository.controller;

import com.fatecrepository.dto.request.DevolverProjetoRequest;
import com.fatecrepository.dto.request.ExcluirProjetoRequest;
import com.fatecrepository.dto.request.ProjetoCreateRequest;
import com.fatecrepository.dto.request.RejeitarProjetoRequest;
import com.fatecrepository.dto.response.ExclusaoProjetoResponse;
import com.fatecrepository.dto.response.ProjetoRemovidoResponse;
import com.fatecrepository.dto.response.ProjetoResponse;
import com.fatecrepository.exception.BadRequestException;
import com.fatecrepository.exception.UnauthorizedException;
import com.fatecrepository.security.CustomUserDetails;
import com.fatecrepository.service.ProjetoExclusaoService;
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
    private final ProjetoExclusaoService projetoExclusaoService;

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

    @GetMapping("/removidos")
    @Operation(summary = "Listar projetos removidos pelo usuário logado",
        description = "Retorna os projetos do autor em REMOVIDO, sem PII, com a contagem regressiva para expirar")
    @SecurityRequirement(name = "Bearer JWT")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lista de removidos retornada (vazia quando não houver)"),
        @ApiResponse(responseCode = "401", description = "Não autenticado")
    })
    public ResponseEntity<List<ProjetoRemovidoResponse>> listarRemovidos(
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        if (userDetails == null) {
            throw new UnauthorizedException("Usuário não autenticado");
        }
        log.info("GET /projetos/removidos para o usuário: {}", userDetails.getId());
        return ResponseEntity.ok(projetoExclusaoService.listarRemovidos(userDetails.getId()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obter projeto por ID", description = "Retorna os detalhes de um projeto específico")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Projeto encontrado"),
        @ApiResponse(responseCode = "404", description = "Projeto não encontrado")
    })
    public ResponseEntity<ProjetoResponse> obterPorId(
        @Parameter(description = "ID do projeto") @PathVariable UUID id,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.info("GET /projetos/{}", id);
        ProjetoResponse response = projetoService.obterPorId(id, userDetails);
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
            + "pode aprovar (403 caso contrário; vínculo não é exigido — revise a instituição no front).")
    @SecurityRequirement(name = "Bearer JWT")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Projeto aprovado"),
        @ApiResponse(responseCode = "401", description = "Não autenticado"),
        @ApiResponse(responseCode = "403", description = "Professor não é o responsável indicado"),
        @ApiResponse(responseCode = "404", description = "Projeto não encontrado"),
        @ApiResponse(responseCode = "409", description = "Projeto já avaliado ou avaliação concorrente")
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
        @ApiResponse(responseCode = "403", description = "Professor não é o responsável indicado"),
        @ApiResponse(responseCode = "404", description = "Projeto não encontrado"),
        @ApiResponse(responseCode = "409", description = "Projeto já avaliado ou avaliação concorrente")
    })
    public ResponseEntity<ProjetoResponse> rejeitar(
        @Parameter(description = "ID do projeto") @PathVariable UUID id,
        @Valid @RequestBody RejeitarProjetoRequest request,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        if (userDetails == null) {
            throw new UnauthorizedException("Usuário não autenticado");
        }
        if (request == null) {
            throw new BadRequestException("O motivo da rejeição é obrigatório");
        }
        log.info("PATCH /projetos/{}/rejeitar pelo professor: {}", id, userDetails.getId());
        return ResponseEntity.ok(projetoService.rejeitar(id, userDetails.getId(), request.getMotivo()));
    }

    @PatchMapping("/{id}/devolver")
    @PreAuthorize("hasRole('PROFESSOR')")
    @Operation(summary = "Devolver projeto para alteração pelo autor",
        description = "Devolve um projeto em AGUARDANDO_APROVACAO para ALTERACOES_SOLICITADAS com justificativa obrigatória. "
            + "Somente o professor responsável indicado na submissão pode devolver.")
    @SecurityRequirement(name = "Bearer JWT")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Projeto devolvido com motivo registrado"),
        @ApiResponse(responseCode = "400", description = "Motivo ausente"),
        @ApiResponse(responseCode = "401", description = "Não autenticado"),
        @ApiResponse(responseCode = "403", description = "Professor não é o responsável indicado"),
        @ApiResponse(responseCode = "404", description = "Projeto não encontrado"),
        @ApiResponse(responseCode = "409", description = "Estado que não permite devolução")
    })
    public ResponseEntity<ProjetoResponse> devolver(
        @Parameter(description = "ID do projeto") @PathVariable UUID id,
        @Valid @RequestBody DevolverProjetoRequest request,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        if (userDetails == null) {
            throw new UnauthorizedException("Usuário não autenticado");
        }
        if (request == null || request.getMotivo() == null || request.getMotivo().isBlank()) {
            throw new BadRequestException("O motivo da devolução é obrigatório");
        }
        log.info("PATCH /projetos/{}/devolver pelo professor: {}", id, userDetails.getId());
        return ResponseEntity.ok(projetoService.devolver(id, userDetails.getId(), request.getMotivo()));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Editar e reenviar projeto devolvido",
        description = "Aplica os campos enviados a um projeto em ALTERACOES_SOLICITADAS e o reenvia como REENVIADO. Só o autor.")
    @SecurityRequirement(name = "Bearer JWT")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Projeto reenviado"),
        @ApiResponse(responseCode = "400", description = "Dados da requisição inválidos"),
        @ApiResponse(responseCode = "401", description = "Não autenticado"),
        @ApiResponse(responseCode = "403", description = "Usuário autenticado não é o autor"),
        @ApiResponse(responseCode = "404", description = "Projeto não encontrado"),
        @ApiResponse(responseCode = "409", description = "Projeto não está em ALTERACOES_SOLICITADAS")
    })
    public ResponseEntity<ProjetoResponse> editar(
        @Parameter(description = "ID do projeto") @PathVariable UUID id,
        @Valid @RequestBody ProjetoCreateRequest request,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        if (userDetails == null) {
            throw new UnauthorizedException("Usuário não autenticado");
        }
        log.info("PUT /projetos/{} pelo autor: {}", id, userDetails.getId());
        return ResponseEntity.ok(projetoService.editar(id, userDetails.getId(), request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir projeto pelo autor",
        description = "AGUARDANDO_APROVACAO/REJEITADO: hard delete. APROVADO: soft delete para REMOVIDO, "
            + "com anonimização imediata e reversão pelo autor em até 14 dias.")
    @SecurityRequirement(name = "Bearer JWT")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Projeto excluído ou removido"),
        @ApiResponse(responseCode = "400", description = "Motivo de remoção inválido"),
        @ApiResponse(responseCode = "401", description = "Não autenticado"),
        @ApiResponse(responseCode = "403", description = "Usuário autenticado não é o autor"),
        @ApiResponse(responseCode = "404", description = "Projeto inexistente ou já removido")
    })
    public ResponseEntity<ExclusaoProjetoResponse> excluir(
        @Parameter(description = "ID do projeto") @PathVariable UUID id,
        @jakarta.validation.Valid @RequestBody(required = false) ExcluirProjetoRequest request,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        if (userDetails == null) {
            throw new UnauthorizedException("Usuário não autenticado");
        }
        if (request == null || request.getMotivoRemocao() == null || request.getMotivoRemocao().isBlank()) {
            throw new BadRequestException("Motivo da remoção inválido");
        }
        log.info("DELETE /projetos/{} pelo autor: {}", id, userDetails.getId());
        return ResponseEntity.ok(projetoExclusaoService.excluir(id, userDetails.getId(), request.getMotivoRemocao()));
    }

    @PostMapping("/{id}/reverter")
    @Operation(summary = "Reverter projeto removido pelo autor",
        description = "Restaura estado e dados originais de um projeto REMOVIDO dentro do prazo de 14 dias")
    @SecurityRequirement(name = "Bearer JWT")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Projeto restaurado"),
        @ApiResponse(responseCode = "401", description = "Não autenticado"),
        @ApiResponse(responseCode = "403", description = "Usuário autenticado não é o autor"),
        @ApiResponse(responseCode = "404", description = "Projeto inexistente ou não está REMOVIDO"),
        @ApiResponse(responseCode = "409", description = "Prazo de reversão expirado")
    })
    public ResponseEntity<ProjetoResponse> reverter(
        @Parameter(description = "ID do projeto") @PathVariable UUID id,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        if (userDetails == null) {
            throw new UnauthorizedException("Usuário não autenticado");
        }
        log.info("POST /projetos/{}/reverter pelo autor: {}", id, userDetails.getId());
        return ResponseEntity.ok(projetoExclusaoService.reverter(id, userDetails.getId()));
    }
}
