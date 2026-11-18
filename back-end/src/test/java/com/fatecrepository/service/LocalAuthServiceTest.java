package com.fatecrepository.service;

import com.fatecrepository.dto.request.CadastroConfirmarRequest;
import com.fatecrepository.dto.request.CadastroIniciarRequest;
import com.fatecrepository.dto.request.EmailLoginRequest;
import com.fatecrepository.dto.request.EsqueciSenhaRequest;
import com.fatecrepository.dto.request.RedefinirSenhaRequest;
import com.fatecrepository.dto.response.AuthResponse;
import com.fatecrepository.exception.BadRequestException;
import com.fatecrepository.exception.ConflictException;
import com.fatecrepository.exception.TooManyRequestsException;
import com.fatecrepository.exception.UnauthorizedException;
import com.fatecrepository.mapper.ResponseMapper;
import com.fatecrepository.model.EmailVerificationCode;
import com.fatecrepository.model.User;
import com.fatecrepository.model.UserRole;
import com.fatecrepository.model.VerificationPurpose;
import com.fatecrepository.repository.EmailVerificationCodeRepository;
import com.fatecrepository.repository.UserRepository;
import com.fatecrepository.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("LocalAuthService — cadastro com código, login e recuperação de senha")
class LocalAuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailVerificationCodeRepository verificationCodeRepository;

    @Mock
    private VerificationAttemptService verificationAttemptService;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private AuthEmailSender authEmailSender;

    private PasswordEncoder passwordEncoder;
    private UserProvisioningService userProvisioningService;
    private LocalAuthService localAuthService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        userProvisioningService = new UserProvisioningService(userRepository);
        localAuthService = new LocalAuthService(
            userRepository,
            verificationCodeRepository,
            verificationAttemptService,
            userProvisioningService,
            passwordEncoder,
            jwtTokenProvider,
            authEmailSender,
            new ResponseMapper()
        );
    }

    @Test
    @DisplayName("Iniciar cadastro gera código de 6 dígitos e envia por e-mail sem gravar usuário ainda")
    void iniciarCadastroEnviaCodigoSemCriarUsuario() {
        when(userRepository.findByEmail("ana@gmail.com")).thenReturn(Optional.empty());
        when(verificationCodeRepository.findTopByEmailAndFinalidadeOrderByCriadoEmDesc(
            "ana@gmail.com", VerificationPurpose.CADASTRO
        )).thenReturn(Optional.empty());

        var resp = localAuthService.iniciarCadastro(
            new CadastroIniciarRequest("Ana Souza", "  ANA@GMAIL.COM ", "senhaSegura123")
        );

        assertEquals("ana@gmail.com", resp.getEmail());
        verify(userRepository, never()).save(any());

        ArgumentCaptor<String> codigoCaptor = ArgumentCaptor.forClass(String.class);
        verify(authEmailSender).enviarCodigoVerificacao(
            eq("ana@gmail.com"),
            eq("Ana Souza"),
            codigoCaptor.capture(),
            eq(VerificationPurpose.CADASTRO),
            eq(15)
        );
        assertTrue(codigoCaptor.getValue().matches("^\\d{6}$"));
    }

    @Test
    @DisplayName("Confirmar cadastro classifica @cps.sp.gov.br como PROFESSOR e demais domínios como ALUNO")
    void confirmarCadastroClassificaDominioCorretamente() {
        when(jwtTokenProvider.generateToken(any(User.class))).thenReturn("jwt-novo");
        when(jwtTokenProvider.getExpirationInSeconds()).thenReturn(86400L);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });

        // Caso 1: @cps.sp.gov.br -> PROFESSOR
        EmailVerificationCode codigoProf = new EmailVerificationCode();
        codigoProf.setId(UUID.randomUUID());
        codigoProf.setEmail("carlos@cps.sp.gov.br");
        codigoProf.setFinalidade(VerificationPurpose.CADASTRO);
        codigoProf.setNome("Carlos Docente");
        codigoProf.setSenhaHash(passwordEncoder.encode("minhaSenha123"));
        codigoProf.setCodigoHash(passwordEncoder.encode("654321"));
        codigoProf.setExpiraEm(LocalDateTime.now().plusMinutes(10));

        when(userRepository.findByEmail("carlos@cps.sp.gov.br")).thenReturn(Optional.empty());
        when(verificationCodeRepository.findTopByEmailAndFinalidadeOrderByCriadoEmDesc(
            "carlos@cps.sp.gov.br", VerificationPurpose.CADASTRO
        )).thenReturn(Optional.of(codigoProf));

        AuthResponse respProf = localAuthService.confirmarCadastro(
            new CadastroConfirmarRequest("carlos@cps.sp.gov.br", "654321")
        );
        assertEquals("PROFESSOR", respProf.getRole());

        // Caso 2: qualquer outro domínio (@gmail.com) -> ALUNO
        EmailVerificationCode codigoAluno = new EmailVerificationCode();
        codigoAluno.setId(UUID.randomUUID());
        codigoAluno.setEmail("estudante@gmail.com");
        codigoAluno.setFinalidade(VerificationPurpose.CADASTRO);
        codigoAluno.setNome("Estudante Externo");
        codigoAluno.setSenhaHash(passwordEncoder.encode("minhaSenha123"));
        codigoAluno.setCodigoHash(passwordEncoder.encode("123456"));
        codigoAluno.setExpiraEm(LocalDateTime.now().plusMinutes(10));

        when(userRepository.findByEmail("estudante@gmail.com")).thenReturn(Optional.empty());
        when(verificationCodeRepository.findTopByEmailAndFinalidadeOrderByCriadoEmDesc(
            "estudante@gmail.com", VerificationPurpose.CADASTRO
        )).thenReturn(Optional.of(codigoAluno));

        AuthResponse respAluno = localAuthService.confirmarCadastro(
            new CadastroConfirmarRequest("estudante@gmail.com", "123456")
        );
        assertEquals("ALUNO", respAluno.getRole());
    }

    @Test
    @DisplayName("Iniciar cadastro barra e-mail que já possui senha cadastrada com 409 Conflict")
    void iniciarCadastroRecusaEmailJaCadastrado() {
        User existente = new User();
        existente.setEmail("ana@gmail.com");
        existente.setSenhaHash(passwordEncoder.encode("senhaExistente"));
        when(userRepository.findByEmail("ana@gmail.com")).thenReturn(Optional.of(existente));

        assertThrows(ConflictException.class, () -> localAuthService.iniciarCadastro(
            new CadastroIniciarRequest("Ana", "ana@gmail.com", "novaSenha123")
        ));
    }

    @Test
    @DisplayName("Iniciar cadastro respeita cooldown de 60 segundos com 429 TooManyRequests")
    void iniciarCadastroRespeitaCooldown() {
        EmailVerificationCode recente = new EmailVerificationCode();
        recente.setCriadoEm(LocalDateTime.now().minusSeconds(15));
        when(userRepository.findByEmail("ana@gmail.com")).thenReturn(Optional.empty());
        when(verificationCodeRepository.findTopByEmailAndFinalidadeOrderByCriadoEmDesc(
            "ana@gmail.com", VerificationPurpose.CADASTRO
        )).thenReturn(Optional.of(recente));

        assertThrows(TooManyRequestsException.class, () -> localAuthService.iniciarCadastro(
            new CadastroIniciarRequest("Ana", "ana@gmail.com", "novaSenha123")
        ));
    }

    @Test
    @DisplayName("Confirmar cadastro incrementa tentativas quando código está incorreto")
    void confirmarCadastroRegistraFalhaEmCodigoIncorreto() {
        UUID idCodigo = UUID.randomUUID();
        EmailVerificationCode pendente = new EmailVerificationCode();
        pendente.setId(idCodigo);
        pendente.setEmail("ana@gmail.com");
        pendente.setCodigoHash(passwordEncoder.encode("111222"));
        pendente.setExpiraEm(LocalDateTime.now().plusMinutes(10));

        when(userRepository.findByEmail("ana@gmail.com")).thenReturn(Optional.empty());
        when(verificationCodeRepository.findTopByEmailAndFinalidadeOrderByCriadoEmDesc(
            "ana@gmail.com", VerificationPurpose.CADASTRO
        )).thenReturn(Optional.of(pendente));
        when(verificationAttemptService.registrarFalha(idCodigo, 5)).thenReturn(1);

        assertThrows(BadRequestException.class, () -> localAuthService.confirmarCadastro(
            new CadastroConfirmarRequest("ana@gmail.com", "999999")
        ));

        verify(verificationAttemptService).registrarFalha(idCodigo, 5);
    }

    @Test
    @DisplayName("Login autentica senha válida e rejeita conta sem senha (somente Microsoft) ou senha incorreta")
    void loginValidaCredenciais() {
        User comSenha = new User();
        comSenha.setId(UUID.randomUUID());
        comSenha.setNome("Ana");
        comSenha.setEmail("ana@gmail.com");
        comSenha.setSenhaHash(passwordEncoder.encode("senhaCorreta123"));
        comSenha.setRole(UserRole.ALUNO);

        when(userRepository.findByEmail("ana@gmail.com")).thenReturn(Optional.of(comSenha));
        when(jwtTokenProvider.generateToken(comSenha)).thenReturn("jwt-login");
        when(jwtTokenProvider.getExpirationInSeconds()).thenReturn(86400L);

        AuthResponse ok = localAuthService.login(new EmailLoginRequest("ana@gmail.com", "senhaCorreta123"));
        assertNotNull(ok);
        assertEquals("jwt-login", ok.getAccessToken());

        assertThrows(UnauthorizedException.class,
            () -> localAuthService.login(new EmailLoginRequest("ana@gmail.com", "errada")));

        User semSenhaMicrosoft = new User();
        semSenhaMicrosoft.setEmail("msal@cps.sp.gov.br");
        semSenhaMicrosoft.setRole(UserRole.PROFESSOR);
        when(userRepository.findByEmail("msal@cps.sp.gov.br")).thenReturn(Optional.of(semSenhaMicrosoft));

        assertThrows(UnauthorizedException.class,
            () -> localAuthService.login(new EmailLoginRequest("msal@cps.sp.gov.br", "qualquerSenha")));
    }

    @Test
    @DisplayName("Recuperação de senha não revela inexistência de e-mail e permite redefinir senha quando válido")
    void recuperacaoDeSenhaCompleta() {
        when(userRepository.findByEmail("inexistente@gmail.com")).thenReturn(Optional.empty());
        var respInexistente = localAuthService.iniciarRecuperacaoSenha(
            new EsqueciSenhaRequest("inexistente@gmail.com")
        );
        assertNotNull(respInexistente.getMensagem());
        verify(authEmailSender, never()).enviarCodigoVerificacao(any(), any(), any(), any(), any(Integer.class));

        User existente = new User();
        existente.setId(UUID.randomUUID());
        existente.setNome("Docente CPS");
        existente.setEmail("docente@cps.sp.gov.br");
        existente.setRole(UserRole.PROFESSOR);

        EmailVerificationCode codigoRec = new EmailVerificationCode();
        codigoRec.setId(UUID.randomUUID());
        codigoRec.setEmail("docente@cps.sp.gov.br");
        codigoRec.setFinalidade(VerificationPurpose.RECUPERACAO_SENHA);
        codigoRec.setCodigoHash(passwordEncoder.encode("777888"));
        codigoRec.setExpiraEm(LocalDateTime.now().plusMinutes(10));

        when(userRepository.findByEmail("docente@cps.sp.gov.br")).thenReturn(Optional.of(existente));
        when(verificationCodeRepository.findTopByEmailAndFinalidadeOrderByCriadoEmDesc(
            "docente@cps.sp.gov.br", VerificationPurpose.RECUPERACAO_SENHA
        )).thenReturn(Optional.of(codigoRec));

        localAuthService.redefinirSenha(
            new RedefinirSenhaRequest("docente@cps.sp.gov.br", "777888", "novaSenhaForte99")
        );

        assertTrue(passwordEncoder.matches("novaSenhaForte99", existente.getSenhaHash()));
        verify(userRepository).save(existente);
    }
}
