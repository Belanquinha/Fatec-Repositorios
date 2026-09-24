package com.fatecrepository.controller;

import com.fatecrepository.dto.response.ProfessorLookupResponse;
import com.fatecrepository.model.User;
import com.fatecrepository.model.UserRole;
import com.fatecrepository.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
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
@RequestMapping("/professores")
@RequiredArgsConstructor
@Tag(name = "Professores", description = "Endpoints para consulta de professores do catálogo/tenant")
public class ProfessorController {

    private final UserRepository userRepository;

    @GetMapping
    @Operation(summary = "Listar professores para autocomplete", description = "Retorna lista de professores cadastrados no sistema para seleção de professor responsável")
    public ResponseEntity<List<ProfessorLookupResponse>> listarProfessores() {
        log.info("GET /professores");
        List<User> professores = userRepository.findByRole(UserRole.PROFESSOR);
        List<ProfessorLookupResponse> response = professores.stream()
            .map(p -> new ProfessorLookupResponse(p.getId(), p.getNome(), p.getEmail()))
            .toList();
        return ResponseEntity.ok(response);
    }
}
