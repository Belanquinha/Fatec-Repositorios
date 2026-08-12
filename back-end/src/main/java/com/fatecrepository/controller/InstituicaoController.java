package com.fatecrepository.controller;

import com.fatecrepository.dto.request.InstituicaoRequest;
import com.fatecrepository.dto.response.InstituicaoResponse;
import com.fatecrepository.service.InstituicaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/instituicoes")
@Tag(name = "Instituições", description = "Endpoints para gerenciamento de instituições")
public class InstituicaoController {

    @Autowired
    private InstituicaoService instituicaoService;

    @GetMapping
    @Operation(summary = "Listar todas as instituições", description = "Retorna uma lista de todas as instituições cadastradas")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lista de instituições retornada com sucesso"),
        @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    public ResponseEntity<List<InstituicaoResponse>> obterTodas() {
        log.info("GET /instituicoes");
        List<InstituicaoResponse> instituicoes = instituicaoService.obterTodas();
        return ResponseEntity.ok(instituicoes);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obter instituição por ID", description = "Retorna uma instituição específica pelo seu ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Instituição encontrada"),
        @ApiResponse(responseCode = "404", description = "Instituição não encontrada"),
        @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    public ResponseEntity<InstituicaoResponse> obterPorId(
        @Parameter(description = "ID da instituição") @PathVariable UUID id) {
        log.info("GET /instituicoes/{}", id);
        InstituicaoResponse instituicao = instituicaoService.obterPorId(id);
        return ResponseEntity.ok(instituicao);
    }

    @PostMapping
    @Operation(summary = "Criar nova instituição", description = "Cria uma nova instituição no sistema")
    @SecurityRequirement(name = "Bearer JWT")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Instituição criada com sucesso"),
        @ApiResponse(responseCode = "400", description = "Requisição inválida"),
        @ApiResponse(responseCode = "401", description = "Não autorizado"),
        @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    public ResponseEntity<InstituicaoResponse> criar(@Valid @RequestBody InstituicaoRequest request) {
        log.info("POST /instituicoes: {}", request.getNome());
        InstituicaoResponse instituicao = instituicaoService.criar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(instituicao);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar instituição", description = "Atualiza uma instituição existente")
    @SecurityRequirement(name = "Bearer JWT")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Instituição atualizada com sucesso"),
        @ApiResponse(responseCode = "400", description = "Requisição inválida"),
        @ApiResponse(responseCode = "401", description = "Não autorizado"),
        @ApiResponse(responseCode = "404", description = "Instituição não encontrada"),
        @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    public ResponseEntity<InstituicaoResponse> atualizar(
        @Parameter(description = "ID da instituição") @PathVariable UUID id,
        @Valid @RequestBody InstituicaoRequest request) {
        log.info("PUT /instituicoes/{}", id);
        InstituicaoResponse instituicao = instituicaoService.atualizar(id, request);
        return ResponseEntity.ok(instituicao);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Deletar instituição", description = "Remove uma instituição do sistema")
    @SecurityRequirement(name = "Bearer JWT")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Instituição deletada com sucesso"),
        @ApiResponse(responseCode = "401", description = "Não autorizado"),
        @ApiResponse(responseCode = "404", description = "Instituição não encontrada"),
        @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    public ResponseEntity<Void> deletar(@Parameter(description = "ID da instituição") @PathVariable UUID id) {
        log.info("DELETE /instituicoes/{}", id);
        instituicaoService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}

