package com.fatecrepository.controller;

import com.fatecrepository.dto.request.ProjetoCreateRequest;
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
}
