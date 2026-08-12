package com.fatecrepository.controller;

import com.fatecrepository.dto.request.ProfessorRequest;
import com.fatecrepository.dto.response.ProfessorResponse;
import com.fatecrepository.service.ProfessorService;
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
@RequestMapping("/professores")
@Tag(name = "Professores", description = "Endpoints para gerenciamento de professores")
public class ProfessorController {

    @Autowired
    private ProfessorService professorService;

    @GetMapping
    @Operation(summary = "Listar todos os professores", description = "Retorna uma lista de todos os professores cadastrados")
    @SecurityRequirement(name = "Bearer JWT")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lista de professores retornada com sucesso"),
        @ApiResponse(responseCode = "401", description = "Não autorizado"),
        @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    public ResponseEntity<List<ProfessorResponse>> obterTodos() {
        log.info("GET /professores");
        List<ProfessorResponse> professores = professorService.obterTodos();
        return ResponseEntity.ok(professores);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obter professor por ID", description = "Retorna um professor específico pelo seu ID")
    @SecurityRequirement(name = "Bearer JWT")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Professor encontrado"),
        @ApiResponse(responseCode = "401", description = "Não autorizado"),
        @ApiResponse(responseCode = "404", description = "Professor não encontrado"),
        @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    public ResponseEntity<ProfessorResponse> obterPorId(
        @Parameter(description = "ID do professor") @PathVariable UUID id) {
        log.info("GET /professores/{}", id);
        ProfessorResponse professor = professorService.obterPorId(id);
        return ResponseEntity.ok(professor);
    }

    @PostMapping
    @Operation(summary = "Criar novo professor", description = "Cria um novo professor no sistema")
    @SecurityRequirement(name = "Bearer JWT")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Professor criado com sucesso"),
        @ApiResponse(responseCode = "400", description = "Requisição inválida"),
        @ApiResponse(responseCode = "401", description = "Não autorizado"),
        @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    public ResponseEntity<ProfessorResponse> criar(@Valid @RequestBody ProfessorRequest request) {
        log.info("POST /professores: {}", request.getEmail());
        ProfessorResponse professor = professorService.criar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(professor);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar professor", description = "Atualiza um professor existente")
    @SecurityRequirement(name = "Bearer JWT")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Professor atualizado com sucesso"),
        @ApiResponse(responseCode = "400", description = "Requisição inválida"),
        @ApiResponse(responseCode = "401", description = "Não autorizado"),
        @ApiResponse(responseCode = "404", description = "Professor não encontrado"),
        @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    public ResponseEntity<ProfessorResponse> atualizar(
        @Parameter(description = "ID do professor") @PathVariable UUID id,
        @Valid @RequestBody ProfessorRequest request) {
        log.info("PUT /professores/{}", id);
        ProfessorResponse professor = professorService.atualizar(id, request);
        return ResponseEntity.ok(professor);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Deletar professor", description = "Remove um professor do sistema")
    @SecurityRequirement(name = "Bearer JWT")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Professor deletado com sucesso"),
        @ApiResponse(responseCode = "401", description = "Não autorizado"),
        @ApiResponse(responseCode = "404", description = "Professor não encontrado"),
        @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    public ResponseEntity<Void> deletar(@Parameter(description = "ID do professor") @PathVariable UUID id) {
        log.info("DELETE /professores/{}", id);
        professorService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}

