package com.fatecrepository.controller;

import com.fatecrepository.dto.request.LoginRequest;
import com.fatecrepository.dto.request.RegisterAlunoRequest;
import com.fatecrepository.dto.request.RegisterProfessorRequest;
import com.fatecrepository.dto.response.AuthResponse;
import com.fatecrepository.model.User;
import com.fatecrepository.model.UserRole;
import com.fatecrepository.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping("/auth")
@Tag(name = "Autenticação", description = "Endpoints de autenticação e registro de usuários")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Realizar login", description = "Autentica um usuário e retorna um token JWT")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Login realizado com sucesso"),
        @ApiResponse(responseCode = "400", description = "Requisição inválida"),
        @ApiResponse(responseCode = "401", description = "Usuário ou senha inválidos"),
        @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("POST /auth/login para email: {}", request.getEmail());
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register/aluno")
    @Operation(summary = "Registrar novo aluno", description = "Cria uma nova conta de aluno e retorna um token JWT")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Aluno registrado com sucesso"),
        @ApiResponse(responseCode = "400", description = "Requisição inválida ou email já cadastrado"),
        @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    public ResponseEntity<AuthResponse> registerAluno(@Valid @RequestBody RegisterAlunoRequest request) {
        log.info("POST /auth/register/aluno para email: {}", request.getEmail());
        AuthResponse response = authService.registerAluno(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/register/professor")
    @Operation(summary = "Registrar novo professor", description = "Cria uma nova conta de professor e retorna um token JWT")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Professor registrado com sucesso"),
        @ApiResponse(responseCode = "400", description = "Requisição inválida ou email já cadastrado"),
        @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    public ResponseEntity<AuthResponse> registerProfessor(@Valid @RequestBody RegisterProfessorRequest request) {
        log.info("POST /auth/register/professor para email: {}", request.getEmail());
        AuthResponse response = authService.registerProfessor(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

}
