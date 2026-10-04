package com.fatecrepository.controller;

import com.fatecrepository.dto.request.DevLoginRequest;
import com.fatecrepository.dto.response.AuthResponse;
import com.fatecrepository.dto.response.DevAuthAccountResponse;
import com.fatecrepository.service.DevAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Login de desenvolvimento, sem Microsoft.
 *
 * <p>As duas anotações de classe são a trava de segurança deste controller: sem o perfil {@code dev}
 * ou sem {@code app.security.dev-auth.enabled=true}, o bean não existe e as rotas respondem 404. Sem
 * elas, estas rotas ficariam expostas — {@code /auth/**} é {@code permitAll} para o POST do login
 * Microsoft, e um JWT emitido aqui abriria a API inteira sem passar por assinatura nem tenant.
 */
@RestController
@RequestMapping("/auth")
@Profile("dev")
@ConditionalOnProperty(name = "app.security.dev-auth.enabled", havingValue = "true")
@Tag(name = "Autenticação (desenvolvimento)", description = "Atalhos de login sem Microsoft. Não existe em produção.")
@RequiredArgsConstructor
public class DevAuthController {

    private final DevAuthService devAuthService;

    @GetMapping("/dev-login/contas")
    @Operation(summary = "Atalhos de login de desenvolvimento")
    public List<DevAuthAccountResponse> contas() {
        return devAuthService.contas();
    }

    @PostMapping("/dev-login")
    @Operation(summary = "Login de desenvolvimento por e-mail")
    public AuthResponse login(@Valid @RequestBody DevLoginRequest request) {
        return devAuthService.login(request.getEmail());
    }
}