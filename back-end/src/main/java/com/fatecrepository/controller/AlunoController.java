package com.fatecrepository.controller;

import com.fatecrepository.dto.request.AlunoRequest;
import com.fatecrepository.dto.response.AlunoResponse;
import com.fatecrepository.service.AlunoService;
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
@RequestMapping("/alunos")
@Tag(name = "Alunos", description = "Endpoints para gerenciamento de alunos")
public class AlunoController {

    @Autowired
    private AlunoService alunoService;

    @GetMapping
    @Operation(summary = "Listar todos os alunos", description = "Retorna uma lista de todos os alunos cadastrados")
    @SecurityRequirement(name = "Bearer JWT")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lista de alunos retornada com sucesso"),
        @ApiResponse(responseCode = "401", description = "Não autorizado"),
        @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    public ResponseEntity<List<AlunoResponse>> obterTodos() {
        log.info("GET /alunos");
        List<AlunoResponse> alunos = alunoService.obterTodos();
        return ResponseEntity.ok(alunos);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obter aluno por ID", description = "Retorna um aluno específico pelo seu ID")
    @SecurityRequirement(name = "Bearer JWT")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Aluno encontrado"),
        @ApiResponse(responseCode = "401", description = "Não autorizado"),
        @ApiResponse(responseCode = "404", description = "Aluno não encontrado"),
        @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    public ResponseEntity<AlunoResponse> obterPorId(
        @Parameter(description = "ID do aluno") @PathVariable UUID id) {
        log.info("GET /alunos/{}", id);
        AlunoResponse aluno = alunoService.obterPorId(id);
        return ResponseEntity.ok(aluno);
    }

    @PostMapping
    @Operation(summary = "Criar novo aluno", description = "Cria um novo aluno no sistema")
    @SecurityRequirement(name = "Bearer JWT")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Aluno criado com sucesso"),
        @ApiResponse(responseCode = "400", description = "Requisição inválida"),
        @ApiResponse(responseCode = "401", description = "Não autorizado"),
        @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    public ResponseEntity<AlunoResponse> criar(@Valid @RequestBody AlunoRequest request) {
        log.info("POST /alunos: {}", request.getEmail());
        AlunoResponse aluno = alunoService.criar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(aluno);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar aluno", description = "Atualiza um aluno existente")
    @SecurityRequirement(name = "Bearer JWT")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Aluno atualizado com sucesso"),
        @ApiResponse(responseCode = "400", description = "Requisição inválida"),
        @ApiResponse(responseCode = "401", description = "Não autorizado"),
        @ApiResponse(responseCode = "404", description = "Aluno não encontrado"),
        @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    public ResponseEntity<AlunoResponse> atualizar(
        @Parameter(description = "ID do aluno") @PathVariable UUID id,
        @Valid @RequestBody AlunoRequest request) {
        log.info("PUT /alunos/{}", id);
        AlunoResponse aluno = alunoService.atualizar(id, request);
        return ResponseEntity.ok(aluno);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Deletar aluno", description = "Remove um aluno do sistema")
    @SecurityRequirement(name = "Bearer JWT")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Aluno deletado com sucesso"),
        @ApiResponse(responseCode = "401", description = "Não autorizado"),
        @ApiResponse(responseCode = "404", description = "Aluno não encontrado"),
        @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    public ResponseEntity<Void> deletar(@Parameter(description = "ID do aluno") @PathVariable UUID id) {
        log.info("DELETE /alunos/{}", id);
        alunoService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}

