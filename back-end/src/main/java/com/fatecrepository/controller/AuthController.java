package com.fatecrepository.controller;

import com.fatecrepository.dto.request.MicrosoftLoginRequest;
import com.fatecrepository.dto.response.AuthResponse;
import com.fatecrepository.model.User;
import com.fatecrepository.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/auth")
@Tag(name = "Autenticação", description = "Endpoints de autenticação via Microsoft")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login-microsoft")
    @Operation(summary = "Login via Microsoft", description = "Autentica um usuário usando token Microsoft e retorna um token JWT")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Login realizado com sucesso"),
        @ApiResponse(responseCode = "401", description = "Token Microsoft inválido"),
        @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    public ResponseEntity<AuthResponse> loginMicrosoft(@Valid @RequestBody MicrosoftLoginRequest request) {
        log.info("POST /auth/login-microsoft");
        AuthResponse response = authService.loginMicrosoft(request);
        return ResponseEntity.ok(response);
    }
}