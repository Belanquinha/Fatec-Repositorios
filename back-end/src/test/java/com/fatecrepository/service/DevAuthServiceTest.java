package com.fatecrepository.service;

import com.fatecrepository.dto.response.AuthResponse;
import com.fatecrepository.dto.response.DevAuthAccountResponse;
import com.fatecrepository.mapper.ResponseMapper;
import com.fatecrepository.model.User;
import com.fatecrepository.model.UserRole;
import com.fatecrepository.security.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * O login de desenvolvimento emite o mesmo JWT do login real, a partir de um e-mail. O que importa
 * verificar aqui é que ele não abre caminho para escolher o papel: o papel vem da regra de domínio,
 * igual no login Microsoft.
 */
@ExtendWith(MockitoExtension.class)
class DevAuthServiceTest {

    @Mock
    private UserProvisioningService userProvisioningService;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Spy
    private ResponseMapper responseMapper = new ResponseMapper();

    @InjectMocks
    private DevAuthService devAuthService;

    @Test
    @DisplayName("Deve emitir um JWT para o e-mail informado, com o papel devolvido pelo provisionamento")
    void deveEmitirJwtParaEmailInformado() {
        quandoProvisionar("maria@cps.sp.gov.br", UserRole.PROFESSOR);

        AuthResponse response = devAuthService.login("maria@cps.sp.gov.br");

        assertEquals("jwt-dev", response.getAccessToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals(86400L, response.getExpiresInSeconds());
        assertEquals("maria@cps.sp.gov.br", response.getEmail());
        assertEquals(UserRole.PROFESSOR.name(), response.getRole());
    }

    @Test
    @DisplayName("Deve passar o e-mail sem normalizar, deixando a normalização para o provisionamento")
    void deveDelegarNormalizacaoAoProvisionamento() {
        quandoProvisionar("maria@cps.sp.gov.br", UserRole.PROFESSOR);

        devAuthService.login("  Maria@CPS.SP.GOV.BR  ");

        // Normalizar em dois lugares criaria duas regras para divergirem. Quem normaliza é o
        // provisionamento, e é o mesmo para os dois caminhos de login.
        verify(userProvisioningService).provisionar("  Maria@CPS.SP.GOV.BR  ", "Maria", null);
    }

    @Test
    @DisplayName("Deve passar foto nula, para não apagar a foto que o Microsoft buscou")
    void devePassarFotoNula() {
        quandoProvisionar("admin@cps.sp.gov.br", UserRole.ADMIN);

        devAuthService.login("admin@cps.sp.gov.br");

        // `provisionar` só escreve foto quando recebe uma não nula; um `null` aqui é o que
        // preserva a foto de quem já logou pelo Microsoft.
        verify(userProvisioningService).provisionar(anyString(), anyString(), isNull());
    }

    @Test
    @DisplayName("Deve derivar o nome do prefixo do e-mail, já que o Graph não devolve displayName")
    void deveDerivarNomeDoPrefixoDoEmail() {
        quandoProvisionar("gabriel@aluno.cps.sp.gov.br", UserRole.ALUNO);

        ArgumentCaptor<String> nome = ArgumentCaptor.forClass(String.class);
        devAuthService.login("gabriel@aluno.cps.sp.gov.br");
        verify(userProvisioningService).provisionar(anyString(), nome.capture(), isNull());

        assertEquals("Gabriel", nome.getValue());
    }

    @Test
    @DisplayName("Deve oferecer um atalho para cada papel, incluindo o e-mail do admin semeado")
    void deveOferecerAtalhosParaCadaPapel() {
        List<DevAuthAccountResponse> contas = devAuthService.contas();

        List<String> emails = contas.stream().map(DevAuthAccountResponse::email).toList();

        // O atalho do admin só funciona se o e-mail bater com o do init/01-create-admin.sql; se
        // divergir, a conta é criada como PROFESSOR pela regra de domínio e o botão mente.
        assertTrue(emails.contains("admin@cps.sp.gov.br"), "falta atalho do admin");
        assertTrue(emails.contains("gabriel@aluno.cps.sp.gov.br"), "falta atalho de aluno");
        assertTrue(emails.contains("maria@cps.sp.gov.br"), "falta atalho de professor");
    }

    @Test
    @DisplayName("Deve gerar o token para o usuário devolvido, e não para um objeto novo")
    void deveGerarTokenParaUsuarioDevolvido() {
        User user = new User();
        user.setNome("Maria");
        user.setEmail("maria@cps.sp.gov.br");
        user.setRole(UserRole.PROFESSOR);
        when(userProvisioningService.provisionar(any(), any(), any())).thenReturn(user);
        when(jwtTokenProvider.generateToken(user)).thenReturn("jwt-dev");
        when(jwtTokenProvider.getExpirationInSeconds()).thenReturn(86400L);

        AuthResponse response = devAuthService.login("maria@cps.sp.gov.br");

        assertEquals("Maria", response.getNome());
        verify(jwtTokenProvider).generateToken(user);
    }

    private void quandoProvisionar(String email, UserRole role) {
        User user = new User();
        user.setNome("Nome de Teste");
        user.setEmail(email);
        user.setRole(role);
        when(userProvisioningService.provisionar(any(), any(), any())).thenReturn(user);
        when(jwtTokenProvider.generateToken(any(User.class))).thenReturn("jwt-dev");
        when(jwtTokenProvider.getExpirationInSeconds()).thenReturn(86400L);
    }
}