package com.fatecrepository.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.fatecrepository.dto.request.MicrosoftLoginRequest;
import com.fatecrepository.dto.response.AuthResponse;
import com.fatecrepository.exception.UnauthorizedException;
import com.fatecrepository.mapper.ResponseMapper;
import com.fatecrepository.model.User;
import com.fatecrepository.model.UserRole;
import com.fatecrepository.repository.UserRepository;
import com.fatecrepository.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String TENANT_CPS = "eabe64c5-68f5-4a76-8301-9577a679e449";
    private static final String TENANT_EXTERNO = "11111111-2222-3333-4444-555555555555";
    private static final String SEGREDO_TESTE = "segredo-de-teste-com-pelo-menos-32-caracteres";

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private MicrosoftGraphService microsoftGraphService;

    @Spy
    private ResponseMapper responseMapper = new ResponseMapper();

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "tenantId", TENANT_CPS);
    }

    @Test
    @DisplayName("Deve emitir JWT para token do tenant da CPS (AD-3)")
    void deveAceitarTokenDoTenantDaCps() {
        prepararLoginComTokenEmitido(graphUser("Aluno da CPS", "gabriel@aluno.cps.sp.gov.br", "gabriel@aluno.cps.sp.gov.br"));

        AuthResponse response = authService.loginMicrosoft(loginComTokenDaCps());

        assertNotNull(response);
        assertEquals("jwt-emitido", response.getAccessToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals("gabriel@aluno.cps.sp.gov.br", response.getEmail());
        assertEquals(UserRole.ALUNO.name(), response.getRole());
        verify(microsoftGraphService, times(1)).getUserInfo(any());
    }

    @Test
    @DisplayName("Deve recusar token de outro tenant com 401 sem chamar o Graph nem persistir (AD-3)")
    void deveRecusarTokenDeTenantExternoSemChamarOGraph() {
        MicrosoftLoginRequest request = loginComToken(tokenComTenant(TENANT_EXTERNO));

        assertThrows(UnauthorizedException.class, () -> authService.loginMicrosoft(request));

        verify(microsoftGraphService, never()).getUserInfo(any());
        verify(microsoftGraphService, never()).getPhotoUrl(any());
        verify(userRepository, never()).findByEmail(any());
        verify(userRepository, never()).save(any());
        verify(jwtTokenProvider, never()).generateToken(any());
    }

    @Test
    @DisplayName("Deve resolver o e-mail por userPrincipalName quando a conta não tem mail")
    void deveResolverEmailPorUserPrincipalNameQuandoMailAusente() {
        prepararLoginComTokenEmitido(graphUser("Professor da CPS", null, "maria@cps.sp.gov.br"));

        AuthResponse response = authService.loginMicrosoft(loginComTokenDaCps());

        assertNotNull(response);
        assertEquals("jwt-emitido", response.getAccessToken());
        assertEquals("maria@cps.sp.gov.br", response.getEmail());
        assertEquals(UserRole.PROFESSOR.name(), response.getRole());
    }

    @Test
    @DisplayName("Deve resolver o e-mail por userPrincipalName quando mail vem em branco")
    void deveResolverEmailPorUserPrincipalNameQuandoMailEmBranco() {
        prepararLoginComTokenEmitido(graphUser("Professor da CPS", "   ", "maria@cps.sp.gov.br"));

        AuthResponse response = authService.loginMicrosoft(loginComTokenDaCps());

        assertNotNull(response);
        assertEquals("maria@cps.sp.gov.br", response.getEmail());
        assertEquals(UserRole.PROFESSOR.name(), response.getRole());
    }

    @Test
    @DisplayName("Deve aparar e normalizar o e-mail antes de classificar o papel e persistir")
    void deveApararENormalizarEmailAntesDeClassificar() {
        prepararLoginComTokenEmitido(graphUser("Professor da CPS", "  MARIA@CPS.SP.GOV.BR  ", null));

        AuthResponse response = authService.loginMicrosoft(loginComTokenDaCps());

        assertEquals("maria@cps.sp.gov.br", response.getEmail());
        assertEquals(UserRole.PROFESSOR.name(), response.getRole());
    }

    @Test
    @DisplayName("Deve recusar o login quando a conta não tem mail nem userPrincipalName")
    void deveRecusarLoginSemEmailNemUpn() {
        when(microsoftGraphService.getUserInfo(any())).thenReturn(graphUser("Conta Sem E-mail", null, null));

        assertThrows(UnauthorizedException.class, () -> authService.loginMicrosoft(loginComTokenDaCps()));

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
    @DisplayName("Deve aceitar token sem claim tid: o app registration single-tenant já garante a origem")
    void deveAceitarTokenSemClaimTid() {
        prepararLoginComTokenEmitido(graphUser("Aluno da CPS", "gabriel@aluno.cps.sp.gov.br", "gabriel@aluno.cps.sp.gov.br"));

        String tokenSemTid = JWT.create().sign(Algorithm.HMAC256(SEGREDO_TESTE));

        AuthResponse response = authService.loginMicrosoft(new MicrosoftLoginRequest(tokenSemTid, null, null, null));

        assertNotNull(response);
        assertEquals("jwt-emitido", response.getAccessToken());
    }

    @Test
    @DisplayName("Deve aceitar token cujo tid difere apenas em maiúsculas e minúsculas")
    void deveCompararTidIgnorandoCase() {
        prepararLoginComTokenEmitido(graphUser("Aluno da CPS", "gabriel@aluno.cps.sp.gov.br", "gabriel@aluno.cps.sp.gov.br"));

        // A comparação precisa ser case-insensitive: o 'tid' do token vem com a grafia do Emissor.
        MicrosoftLoginRequest request = loginComToken(tokenComTenant(TENANT_CPS.toUpperCase()));

        AuthResponse response = authService.loginMicrosoft(request);

        assertNotNull(response);
        assertEquals("jwt-emitido", response.getAccessToken());
    }

    @Test
    @DisplayName("Deve classificar como ALUNO quando o domínio não é da CPS (fallback de AD-2)")
    void deveClassificarComoAlunoQuandoDominioNaoEhDaCps() {
        prepararLoginComTokenEmitido(graphUser("Visitante", "externo@exemplo.com", "externo@exemplo.com"));

        AuthResponse response = authService.loginMicrosoft(loginComTokenDaCps());

        assertEquals(UserRole.ALUNO.name(), response.getRole());
    }

    @Test
    @DisplayName("Deve classificar como ALUNO quando o domínio é o de aluno da CPS")
    void deveClassificarComoAlunoQuandoDominioEhDeAluno() {
        prepararLoginComTokenEmitido(graphUser("Aluno da CPS", "gabriel@aluno.cps.sp.gov.br", "gabriel@aluno.cps.sp.gov.br"));

        AuthResponse response = authService.loginMicrosoft(loginComTokenDaCps());

        assertEquals(UserRole.ALUNO.name(), response.getRole());
    }

    private void prepararLoginComTokenEmitido(MicrosoftGraphService.GraphUser graphUser) {
        when(microsoftGraphService.getUserInfo(any())).thenReturn(graphUser);
        when(microsoftGraphService.getPhotoUrl(any())).thenReturn("data:image/jpeg;base64,QUJD");
        when(userRepository.findByEmail(any())).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocacao -> {
            User user = invocacao.getArgument(0);
            user.setId(UUID.randomUUID());
            return user;
        });
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
        return new MicrosoftLoginRequest(token, null, null, null);
    }

    private String tokenComTenant(String tid) {
        return JWT.create()
            .withClaim("tid", tid)
            .sign(Algorithm.HMAC256(SEGREDO_TESTE));
    }
}
