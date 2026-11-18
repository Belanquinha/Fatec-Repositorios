package com.fatecrepository.controller;

import com.fatecrepository.dto.response.CursoResponse;
import com.fatecrepository.service.CursoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/cursos")
@RequiredArgsConstructor
@Tag(name = "Cursos", description = "Endpoints públicos para consulta do catálogo de cursos das Fatecs")
public class CursoController {

    private final CursoService cursoService;

    @GetMapping
    @Operation(summary = "Listar cursos ativos", description = "Retorna todos os cursos ativos do catálogo ordenados alfabeticamente")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lista de cursos retornada com sucesso")
    })
    public ResponseEntity<List<CursoResponse>> obterTodos() {
        log.info("GET /cursos");
        return ResponseEntity.ok(cursoService.obterTodosAtivos());
    }
}
