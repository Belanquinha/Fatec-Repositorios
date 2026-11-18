package com.fatecrepository.controller;

import com.fatecrepository.dto.request.CadastroConfirmarRequest;
import com.fatecrepository.dto.request.CadastroIniciarRequest;
import com.fatecrepository.dto.request.EmailLoginRequest;
import com.fatecrepository.dto.request.EsqueciSenhaRequest;
import com.fatecrepository.dto.request.MicrosoftLoginRequest;
import com.fatecrepository.dto.request.RedefinirSenhaRequest;
import com.fatecrepository.dto.request.ReenviarCodigoRequest;
import com.fatecrepository.dto.response.AuthResponse;
import com.fatecrepository.dto.response.CodigoEnviadoResponse;
import com.fatecrepository.dto.response.MensagemResponse;
import com.fatecrepository.service.AuthService;
import com.fatecrepository.service.LocalAuthService;
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
@Tag(name = "Autenticação", description = "Endpoints de autenticação por e-mail/senha, cadastro com código e SSO Microsoft")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final LocalAuthService localAuthService;

    @PostMapping("/login-microsoft")
    @Operation(summary = "Login via Microsoft", description = "Autentica um usuário institucional CPS usando token Microsoft e retorna um token JWT")
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

    @PostMapping("/login")
    @Operation(summary = "Login por e-mail e senha", description = "Autentica um usuário cadastrado com e-mail e senha e retorna um token JWT")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Login realizado com sucesso"),
        @ApiResponse(responseCode = "400", description = "Dados inválidos"),
        @ApiResponse(responseCode = "401", description = "Credenciais inválidas")
    })
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody EmailLoginRequest request) {
        log.info("POST /auth/login");
        return ResponseEntity.ok(localAuthService.login(request));
    }

    @PostMapping("/cadastro/iniciar")
    @Operation(summary = "Iniciar cadastro", description = "Registra cadastro pendente e envia código de 6 dígitos para confirmar existência do e-mail")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Código enviado para o e-mail"),
        @ApiResponse(responseCode = "400", description = "Dados inválidos"),
        @ApiResponse(responseCode = "409", description = "E-mail já cadastrado"),
        @ApiResponse(responseCode = "429", description = "Aguarde antes de solicitar novo código"),
        @ApiResponse(responseCode = "503", description = "Falha no envio do e-mail")
    })
    public ResponseEntity<CodigoEnviadoResponse> iniciarCadastro(@Valid @RequestBody CadastroIniciarRequest request) {
        log.info("POST /auth/cadastro/iniciar");
        return ResponseEntity.ok(localAuthService.iniciarCadastro(request));
    }

    @PostMapping("/cadastro/reenviar")
    @Operation(summary = "Reenviar código de cadastro", description = "Envia um novo código de 6 dígitos para um cadastro pendente")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Novo código enviado"),
        @ApiResponse(responseCode = "400", description = "Sem cadastro pendente"),
        @ApiResponse(responseCode = "429", description = "Aguarde antes de solicitar novo código")
    })
    public ResponseEntity<CodigoEnviadoResponse> reenviarCodigoCadastro(@Valid @RequestBody ReenviarCodigoRequest request) {
        log.info("POST /auth/cadastro/reenviar");
        return ResponseEntity.ok(localAuthService.reenviarCodigoCadastro(request));
    }

    @PostMapping("/cadastro/confirmar")
    @Operation(summary = "Confirmar cadastro com código", description = "Valida o código de 6 dígitos, cria a conta classificando o papel pelo domínio e retorna sessão JWT")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Conta confirmada e autenticada"),
        @ApiResponse(responseCode = "400", description = "Código inválido ou expirado"),
        @ApiResponse(responseCode = "409", description = "E-mail já cadastrado")
    })
    public ResponseEntity<AuthResponse> confirmarCadastro(@Valid @RequestBody CadastroConfirmarRequest request) {
        log.info("POST /auth/cadastro/confirmar");
        return ResponseEntity.ok(localAuthService.confirmarCadastro(request));
    }

    @PostMapping("/senha/esquecer")
    @Operation(summary = "Solicitar código de recuperação de senha", description = "Envia um código de 6 dígitos para o e-mail caso possua conta na plataforma")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Solicitação processada")
    })
    public ResponseEntity<CodigoEnviadoResponse> esquecerSenha(@Valid @RequestBody EsqueciSenhaRequest request) {
        log.info("POST /auth/senha/esquecer");
        return ResponseEntity.ok(localAuthService.iniciarRecuperacaoSenha(request));
    }

    @PostMapping("/senha/redefinir")
    @Operation(summary = "Redefinir senha com código", description = "Valida o código de 6 dígitos recebido por e-mail e grava a nova senha")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Senha redefinida com sucesso"),
        @ApiResponse(responseCode = "400", description = "Código inválido ou expirado")
    })
    public ResponseEntity<MensagemResponse> redefinirSenha(@Valid @RequestBody RedefinirSenhaRequest request) {
        log.info("POST /auth/senha/redefinir");
        return ResponseEntity.ok(localAuthService.redefinirSenha(request));
    }
}
