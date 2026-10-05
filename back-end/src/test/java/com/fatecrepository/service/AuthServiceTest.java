package com.fatecrepository.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.fatecrepository.dto.request.MicrosoftLoginRequest;
import com.fatecrepository.dto.response.AuthResponse;
import com.fatecrepository.exception.UnauthorizedException;
import com.fatecrepository.mapper.ResponseMapper;
import com.fatecrepository.model.User;
import com.fatecrepository.model.UserRole;
import com.fatecrepository.security.JwtTokenProvider;
import com.fatecrepository.security.MicrosoftTokenVerifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Foco no login Microsoft: o que é verificado antes de qualquer chamada externa, e qual e-mail é
 * extraído do Graph.
 *
 * <p>A classificação de papel por domínio não é testada aqui — ela foi para
 * {@code UserProvisioningServiceTest}, junto com o dono da regra. Aqui o
 * {@code UserProvisioningService} é mock, e o que se verifica é <em>qual</em> e-mail chega até ele.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String TENANT_CPS = "eabe64c5-68f5-4a76-8301-9577a679e449";
    private static final String SEGREDO_TESTE = "segredo-de-teste-com-pelo-menos-32-caracteres";

    private static final String FOTO = "data:image/jpeg;base64,QUJD";

    @Mock
    private UserProvisioningService userProvisioningService;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private MicrosoftGraphService microsoftGraphService;

    @Mock
    private MicrosoftTokenVerifier tokenVerifier;

    @Spy
    private ResponseMapper responseMapper = new ResponseMapper();

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        // A verificação real de assinatura/tenant é coberta por MicrosoftTokenVerifierTest; aqui
        // o verificador é mock para manter o foco no e-mail extraído do Graph.
        // lenient() porque deveAbortarQuandoVerificadorRecusaToken substitui este stub por
        // doThrow(...) — sem leniência o MockitoExtension acusa o stub como desnecessário.
        ReflectionTestUtils.setField(authService, "tokenVerifier", tokenVerifier);
        org.mockito.Mockito.lenient().doNothing().when(tokenVerifier).verify(any());
    }

    @Test
    @DisplayName("Deve emitir JWT para token do tenant da CPS (AD-3)")
    void deveAceitarTokenDoTenantDaCps() {
        prepararLoginComTokenEmitido(graphUser("Aluno da CPS", "gabriel@aluno.cps.sp.gov.br", "gabriel@aluno.cps.sp.gov.br"), "gabriel@aluno.cps.sp.gov.br", UserRole.ALUNO);

        AuthResponse response = authService.loginMicrosoft(loginComTokenDaCps());

        assertNotNull(response);
        assertEquals("jwt-emitido", response.getAccessToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals("gabriel@aluno.cps.sp.gov.br", response.getEmail());
        assertEquals(UserRole.ALUNO.name(), response.getRole());
        verify(microsoftGraphService, times(1)).getUserInfo(any());
    }

    @Test
    @DisplayName("Deve abortar com 401 quando o verificador recusa o token, sem chamar o Graph nem persistir (AD-3)")
    void deveAbortarQuandoVerificadorRecusaToken() {
        org.mockito.Mockito.doThrow(new UnauthorizedException("Tenant inválido: o token não pertence ao tenant da CPS"))
                .when(tokenVerifier).verifyIdToken(any());
        MicrosoftLoginRequest request = loginComTokenDaCps();

        assertThrows(UnauthorizedException.class, () -> authService.loginMicrosoft(request));

        verify(microsoftGraphService, never()).getUserInfo(any());
        verify(microsoftGraphService, never()).getPhotoUrl(any());
        verify(userProvisioningService, never()).provisionar(any(), any(), any());
        verify(jwtTokenProvider, never()).generateToken(any());
    }

    @Test
    @DisplayName("Deve resolver o e-mail por userPrincipalName quando a conta não tem mail")
    void deveResolverEmailPorUserPrincipalNameQuandoMailAusente() {
        prepararLoginComTokenEmitido(graphUser("Professor da CPS", null, "maria@cps.sp.gov.br"), "maria@cps.sp.gov.br", UserRole.PROFESSOR);

        AuthResponse response = authService.loginMicrosoft(loginComTokenDaCps());

        assertNotNull(response);
        assertEquals("jwt-emitido", response.getAccessToken());
        assertEquals("maria@cps.sp.gov.br", response.getEmail());
        verify(userProvisioningService).provisionar(eq("maria@cps.sp.gov.br"), any(), any());
    }

    @Test
    @DisplayName("Deve resolver o e-mail por userPrincipalName quando mail vem em branco")
    void deveResolverEmailPorUserPrincipalNameQuandoMailEmBranco() {
        prepararLoginComTokenEmitido(graphUser("Professor da CPS", "   ", "maria@cps.sp.gov.br"), "maria@cps.sp.gov.br", UserRole.PROFESSOR);

        AuthResponse response = authService.loginMicrosoft(loginComTokenDaCps());

        assertNotNull(response);
        assertEquals("jwt-emitido", response.getAccessToken());
        assertEquals("maria@cps.sp.gov.br", response.getEmail());
        verify(userProvisioningService).provisionar(eq("maria@cps.sp.gov.br"), any(), any());
    }

    @Test
    @DisplayName("Deve aparar e normalizar o e-mail antes de classificar o papel e persistir")
    void deveApararENormalizarEmailAntesDeClassificar() {
        prepararLoginComTokenEmitido(graphUser("Professor da CPS", "  MARIA@CPS.SP.GOV.BR  ", null), "maria@cps.sp.gov.br", UserRole.PROFESSOR);

        AuthResponse response = authService.loginMicrosoft(loginComTokenDaCps());

        assertEquals("maria@cps.sp.gov.br", response.getEmail());
        // A classificação compara por `endsWith` e a coluna de e-mail é única: um e-mail com caixa
        // alta ou espaço passaria direto e criaria um segundo usuário para o mesmo endereço.
        verify(userProvisioningService).provisionar(eq("maria@cps.sp.gov.br"), any(), any());
    }

    @Test
    @DisplayName("Deve passar ao provisionamento o nome do Graph e a foto buscada")
    void deveRepassarNomeEFotoAoProvisionamento() {
        prepararLoginComTokenEmitido(graphUser("Maria da CPS", "maria@cps.sp.gov.br", null), "maria@cps.sp.gov.br", UserRole.PROFESSOR);

        authService.loginMicrosoft(loginComTokenDaCps());

        verify(userProvisioningService).provisionar("maria@cps.sp.gov.br", "Maria da CPS", FOTO);
    }

    @Test
    @DisplayName("Deve recusar o login quando a conta não tem mail nem userPrincipalName")
    void deveRecusarLoginSemEmailNemUpn() {
        when(microsoftGraphService.getUserInfo(any())).thenReturn(graphUser("Conta Sem E-mail", null, null));

        assertThrows(UnauthorizedException.class, () -> authService.loginMicrosoft(loginComTokenDaCps()));

        verify(userProvisioningService, never()).provisionar(any(), any(), any());
        verify(jwtTokenProvider, never()).generateToken(any());
    }

    @Test
    @DisplayName("Deve recusar o login quando o Graph não devolve dados do usuário")
    void deveRecusarLoginQuandoGraphRetornaNulo() {
        when(microsoftGraphService.getUserInfo(any())).thenReturn(null);

        assertThrows(
            UnauthorizedException.class,
            () -> authService.loginMicrosoft(loginComTokenDaCps())
        );
    }

    @Test
    @DisplayName("Deve repassar o access token recebido ao verificador")
    void deveRepassarTokenAoVerificador() {
        prepararLoginComTokenEmitido(graphUser("Aluno da CPS", "gabriel@aluno.cps.sp.gov.br", "gabriel@aluno.cps.sp.gov.br"), "gabriel@aluno.cps.sp.gov.br", UserRole.ALUNO);

        MicrosoftLoginRequest request = loginComTokenDaCps();
        authService.loginMicrosoft(request);

        verify(tokenVerifier).verify(request.getAccessToken());
    }

    private void prepararLoginComTokenEmitido(MicrosoftGraphService.GraphUser graphUser, String emailEsperado, UserRole role) {
        when(microsoftGraphService.getUserInfo(any())).thenReturn(graphUser);
        when(microsoftGraphService.getPhotoUrl(any())).thenReturn(FOTO);

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setNome(graphUser.getDisplayName());
        user.setEmail(emailEsperado);
        user.setRole(role);
        when(userProvisioningService.provisionar(any(), any(), any())).thenReturn(user);

        when(jwtTokenProvider.generateToken(any(User.class))).thenReturn("jwt-emitido");
        when(jwtTokenProvider.getExpirationInSeconds()).thenReturn(86400L);
    }

    private MicrosoftGraphService.GraphUser graphUser(String displayName, String mail, String userPrincipalName) {
        MicrosoftGraphService.GraphUser graphUser = new MicrosoftGraphService.GraphUser();
        graphUser.setId(UUID.randomUUID().toString());
        graphUser.setDisplayName(displayName);
        graphUser.setMail(mail);
        graphUser.setUserPrincipalName(userPrincipalName);
        return graphUser;
    }

    private MicrosoftLoginRequest loginComTokenDaCps() {
        return loginComToken(tokenComTenant(TENANT_CPS));
    }

    private MicrosoftLoginRequest loginComToken(String token) {
        return new MicrosoftLoginRequest(token, "id-token-teste", null, null, null);
    }

    private String tokenComTenant(String tid) {
        return JWT.create()
            .withClaim("tid", tid)
            .sign(Algorithm.HMAC256(SEGREDO_TESTE));
    }
}