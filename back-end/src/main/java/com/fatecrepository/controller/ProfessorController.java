package com.fatecrepository.controller;

import com.fatecrepository.dto.request.VinculoInstituicoesRequest;
import com.fatecrepository.dto.response.InstituicaoResponse;
import com.fatecrepository.dto.response.ProfessorDetalheResponse;
import com.fatecrepository.dto.response.ProfessorLookupResponse;
import com.fatecrepository.exception.BadRequestException;
import com.fatecrepository.exception.ResourceNotFoundException;
import com.fatecrepository.exception.UnauthorizedException;
import com.fatecrepository.model.User;
import com.fatecrepository.model.UserRole;
import com.fatecrepository.repository.UserRepository;
import com.fatecrepository.security.CustomUserDetails;
import com.fatecrepository.service.ProfessorVinculoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/professores")
@RequiredArgsConstructor
@Tag(name = "Professores", description = "Endpoints para consulta de professores do catálogo/tenant")
public class ProfessorController {

    private final UserRepository userRepository;
    private final ProfessorVinculoService vinculoService;

    @GetMapping
    @SecurityRequirement(name = "Bearer JWT")
    @Operation(summary = "Buscar professores para autocomplete (LGPD)",
        description = "Busca autenticada por nome (mín. 2 caracteres, máx. 10 resultados). "
            + "A lista expõe só id+nome — o e-mail sai apenas no detalhe /professores/{id} "
            + "do professor selecionado. Sem ?q (ou q curto) retorna [].")
    public ResponseEntity<List<ProfessorLookupResponse>> listarProfessores(
        @RequestParam(value = "q", required = false) String q) {
        String termo = q == null ? "" : q.trim();
        if (termo.length() < 2) {
            return ResponseEntity.ok(List.of());
        }
        List<User> professores =
            userRepository.findTop10ByRoleAndNomeContainingIgnoreCaseOrderByNomeAsc(UserRole.PROFESSOR, termo);
        List<ProfessorLookupResponse> response = professores.stream()
            .map(p -> new ProfessorLookupResponse(p.getId(), p.getNome()))
            .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @SecurityRequirement(name = "Bearer JWT")
    @Operation(summary = "Detalhe do professor selecionado",
        description = "Retorna id+nome+e-mail de UM professor (para preencher "
            + "emailProfessorResponsavel após seleção no autocomplete). Exige autenticação.")
    public ResponseEntity<ProfessorDetalheResponse> detalharProfessor(@PathVariable UUID id) {
        User professor = userRepository.findById(id)
            .filter(u -> u.getRole() == UserRole.PROFESSOR)
            .orElseThrow(() -> new ResourceNotFoundException("Professor não encontrado"));
        return ResponseEntity.ok(
            new ProfessorDetalheResponse(professor.getId(), professor.getNome(), professor.getEmail()));
    }

    @GetMapping("/me/instituicoes")
    @PreAuthorize("hasRole('PROFESSOR')")
    @SecurityRequirement(name = "Bearer JWT")
    @Operation(summary = "Ler meus vínculos", description = "Retorna as instituições vinculadas ao professor autenticado (0 a 4, ordenadas por nome)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Vínculos retornados (array vazio quando zerado)"),
        @ApiResponse(responseCode = "401", description = "Não autenticado"),
        @ApiResponse(responseCode = "403", description = "Papel diferente de PROFESSOR")
    })
    public ResponseEntity<List<InstituicaoResponse>> lerMeusVinculos(
        @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            throw new UnauthorizedException("Usuário não autenticado");
        }
        log.info("GET /professores/me/instituicoes para {}", userDetails.getId());
        return ResponseEntity.ok(vinculoService.ler(userDetails.getId()));
    }

    @PutMapping("/me/instituicoes")
    @PreAuthorize("hasRole('PROFESSOR')")
    @SecurityRequirement(name = "Bearer JWT")
    @Operation(summary = "Substituir meus vínculos", description = "Substitui o conjunto completo de instituições do professor (0 a 4, idempotente)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Vínculos persistidos"),
        @ApiResponse(responseCode = "400", description = "Body malformado, UUID inválido ou ids duplicados"),
        @ApiResponse(responseCode = "401", description = "Não autenticado"),
        @ApiResponse(responseCode = "403", description = "Papel diferente de PROFESSOR"),
        @ApiResponse(responseCode = "404", description = "Instituição inexistente (nada persistido)"),
        @ApiResponse(responseCode = "409", description = "Mais de 4 ids (teto)")
    })
    public ResponseEntity<List<InstituicaoResponse>> substituirMeusVinculos(
        @Valid @RequestBody VinculoInstituicoesRequest request,
        @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            throw new UnauthorizedException("Usuário não autenticado");
        }
        if (request == null) {
            throw new BadRequestException("instituicaoIds é obrigatório (use [] para limpar os vínculos)");
        }
        log.info("PUT /professores/me/instituicoes para {} com {} ids",
            userDetails.getId(), request.getInstituicaoIds() == null ? 0 : request.getInstituicaoIds().size());
        return ResponseEntity.ok(vinculoService.substituir(userDetails.getId(), request.getInstituicaoIds()));
    }
}
