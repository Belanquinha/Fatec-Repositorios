package com.fatecrepository.service;

import com.fatecrepository.dto.request.CadastroConfirmarRequest;
import com.fatecrepository.dto.request.CadastroIniciarRequest;
import com.fatecrepository.dto.request.EmailLoginRequest;
import com.fatecrepository.dto.request.EsqueciSenhaRequest;
import com.fatecrepository.dto.request.RedefinirSenhaRequest;
import com.fatecrepository.dto.request.ReenviarCodigoRequest;
import com.fatecrepository.dto.response.AuthResponse;
import com.fatecrepository.dto.response.CodigoEnviadoResponse;
import com.fatecrepository.dto.response.MensagemResponse;
import com.fatecrepository.exception.BadRequestException;
import com.fatecrepository.exception.ConflictException;
import com.fatecrepository.exception.TooManyRequestsException;
import com.fatecrepository.exception.UnauthorizedException;
import com.fatecrepository.mapper.ResponseMapper;
import com.fatecrepository.model.EmailVerificationCode;
import com.fatecrepository.model.User;
import com.fatecrepository.model.VerificationPurpose;
import com.fatecrepository.repository.EmailVerificationCodeRepository;
import com.fatecrepository.repository.UserRepository;
import com.fatecrepository.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class LocalAuthService {

    static final int VALIDADE_CODIGO_MINUTOS = 15;
    static final int COOLDOWN_REENVIO_SEGUNDOS = 60;
    static final int MAX_TENTATIVAS_CODIGO = 5;

    private static final String CREDENCIAIS_INVALIDAS = "E-mail ou senha inválidos";
    private static final String MENSAGEM_NEUTRA_RECUPERACAO =
        "Se este e-mail estiver cadastrado na plataforma, enviamos um código de 6 dígitos para redefinir sua senha.";

    private final SecureRandom secureRandom = new SecureRandom();

    private final UserRepository userRepository;
    private final EmailVerificationCodeRepository verificationCodeRepository;
    private final VerificationAttemptService verificationAttemptService;
    private final UserProvisioningService userProvisioningService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthEmailSender authEmailSender;
    private final ResponseMapper responseMapper;

    @Transactional(readOnly = true)
    public AuthResponse login(EmailLoginRequest request) {
        String email = userProvisioningService.normalizarEmail(request.getEmail());
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new UnauthorizedException(CREDENCIAIS_INVALIDAS));

        if (user.getSenhaHash() == null || !passwordEncoder.matches(request.getSenha(), user.getSenhaHash())) {
            throw new UnauthorizedException(CREDENCIAIS_INVALIDAS);
        }

        String token = jwtTokenProvider.generateToken(user);
        log.info("Login por e-mail/senha realizado com sucesso para: {} (papel: {})",
            user.getEmail(), user.getRole().name());

        return emitirSessao(user, token);
    }

    @Transactional
    public CodigoEnviadoResponse iniciarCadastro(CadastroIniciarRequest request) {
        String email = userProvisioningService.normalizarEmail(request.getEmail());
        String nome = request.getNome().trim();

        userRepository.findByEmail(email).ifPresent(existente -> {
            if (existente.getSenhaHash() != null) {
                throw new ConflictException(
                    "Este e-mail já possui cadastro. Faça login ou recupere sua senha."
                );
            }
        });

        verificarCooldown(email, VerificationPurpose.CADASTRO);

        verificationCodeRepository.deleteByEmailAndFinalidade(email, VerificationPurpose.CADASTRO);

        String codigo = gerarCodigoSeisDigitos();
        LocalDateTime agora = LocalDateTime.now();

        EmailVerificationCode registro = new EmailVerificationCode();
        registro.setEmail(email);
        registro.setFinalidade(VerificationPurpose.CADASTRO);
        registro.setNome(nome);
        registro.setSenhaHash(passwordEncoder.encode(request.getSenha()));
        registro.setCodigoHash(passwordEncoder.encode(codigo));
        registro.setTentativas(0);
        registro.setCriadoEm(agora);
        registro.setExpiraEm(agora.plusMinutes(VALIDADE_CODIGO_MINUTOS));

        verificationCodeRepository.save(registro);

        authEmailSender.enviarCodigoVerificacao(
            email,
            nome,
            codigo,
            VerificationPurpose.CADASTRO,
            VALIDADE_CODIGO_MINUTOS
        );

        return new CodigoEnviadoResponse(
            "Enviamos um código de verificação de 6 dígitos para o seu e-mail.",
            email,
            VALIDADE_CODIGO_MINUTOS * 60,
            COOLDOWN_REENVIO_SEGUNDOS
        );
    }

    @Transactional
    public CodigoEnviadoResponse reenviarCodigoCadastro(ReenviarCodigoRequest request) {
        String email = userProvisioningService.normalizarEmail(request.getEmail());

        userRepository.findByEmail(email).ifPresent(existente -> {
            if (existente.getSenhaHash() != null) {
                throw new ConflictException("Este e-mail já foi confirmado. Faça login.");
            }
        });

        EmailVerificationCode pendente = verificationCodeRepository
            .findTopByEmailAndFinalidadeOrderByCriadoEmDesc(email, VerificationPurpose.CADASTRO)
            .orElseThrow(() -> new BadRequestException(
                "Nenhum cadastro pendente encontrado para este e-mail. Preencha os dados novamente."
            ));

        verificarCooldownRegistro(pendente);

        String novoCodigo = gerarCodigoSeisDigitos();
        LocalDateTime agora = LocalDateTime.now();

        pendente.setCodigoHash(passwordEncoder.encode(novoCodigo));
        pendente.setTentativas(0);
        pendente.setCriadoEm(agora);
        pendente.setExpiraEm(agora.plusMinutes(VALIDADE_CODIGO_MINUTOS));
        verificationCodeRepository.save(pendente);

        authEmailSender.enviarCodigoVerificacao(
            email,
            pendente.getNome(),
            novoCodigo,
            VerificationPurpose.CADASTRO,
            VALIDADE_CODIGO_MINUTOS
        );

        return new CodigoEnviadoResponse(
            "Enviamos um novo código de 6 dígitos para o seu e-mail.",
            email,
            VALIDADE_CODIGO_MINUTOS * 60,
            COOLDOWN_REENVIO_SEGUNDOS
        );
    }

    @Transactional
    public AuthResponse confirmarCadastro(CadastroConfirmarRequest request) {
        String email = userProvisioningService.normalizarEmail(request.getEmail());

        userRepository.findByEmail(email).ifPresent(existente -> {
            if (existente.getSenhaHash() != null) {
                throw new ConflictException("Este e-mail já possui cadastro. Faça login.");
            }
        });

        EmailVerificationCode confirmado = validarEConsumirCodigo(
            email,
            request.getCodigo().trim(),
            VerificationPurpose.CADASTRO
        );

        User user = userProvisioningService.provisionarComSenha(
            email,
            confirmado.getNome(),
            confirmado.getSenhaHash()
        );

        String token = jwtTokenProvider.generateToken(user);
        log.info("Cadastro confirmado com sucesso para: {} (papel: {})",
            user.getEmail(), user.getRole().name());

        return emitirSessao(user, token);
    }

    @Transactional
    public CodigoEnviadoResponse iniciarRecuperacaoSenha(EsqueciSenhaRequest request) {
        String email = userProvisioningService.normalizarEmail(request.getEmail());

        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return new CodigoEnviadoResponse(
                MENSAGEM_NEUTRA_RECUPERACAO,
                email,
                VALIDADE_CODIGO_MINUTOS * 60,
                COOLDOWN_REENVIO_SEGUNDOS
            );
        }

        // Em recuperação de senha não expomos 429 apenas para e-mails existentes (evita enumeração):
        // dentro da janela de cooldown, apenas preservamos o código recém-enviado e devolvemos 200.
        var existenteOpt = verificationCodeRepository.findTopByEmailAndFinalidadeOrderByCriadoEmDesc(
            email,
            VerificationPurpose.RECUPERACAO_SENHA
        );
        if (existenteOpt.isPresent()) {
            long segundosDesdeEnvio = Duration.between(
                existenteOpt.get().getCriadoEm(),
                LocalDateTime.now()
            ).getSeconds();
            if (segundosDesdeEnvio < COOLDOWN_REENVIO_SEGUNDOS) {
                return new CodigoEnviadoResponse(
                    MENSAGEM_NEUTRA_RECUPERACAO,
                    email,
                    VALIDADE_CODIGO_MINUTOS * 60,
                    COOLDOWN_REENVIO_SEGUNDOS
                );
            }
        }

        verificationCodeRepository.deleteByEmailAndFinalidade(email, VerificationPurpose.RECUPERACAO_SENHA);

        String codigo = gerarCodigoSeisDigitos();
        LocalDateTime agora = LocalDateTime.now();

        EmailVerificationCode registro = new EmailVerificationCode();
        registro.setEmail(email);
        registro.setFinalidade(VerificationPurpose.RECUPERACAO_SENHA);
        registro.setNome(user.getNome());
        registro.setCodigoHash(passwordEncoder.encode(codigo));
        registro.setTentativas(0);
        registro.setCriadoEm(agora);
        registro.setExpiraEm(agora.plusMinutes(VALIDADE_CODIGO_MINUTOS));

        verificationCodeRepository.save(registro);

        authEmailSender.enviarCodigoVerificacao(
            email,
            user.getNome(),
            codigo,
            VerificationPurpose.RECUPERACAO_SENHA,
            VALIDADE_CODIGO_MINUTOS
        );

        return new CodigoEnviadoResponse(
            MENSAGEM_NEUTRA_RECUPERACAO,
            email,
            VALIDADE_CODIGO_MINUTOS * 60,
            COOLDOWN_REENVIO_SEGUNDOS
        );
    }

    @Transactional
    public MensagemResponse redefinirSenha(RedefinirSenhaRequest request) {
        String email = userProvisioningService.normalizarEmail(request.getEmail());

        validarEConsumirCodigo(
            email,
            request.getCodigo().trim(),
            VerificationPurpose.RECUPERACAO_SENHA
        );

        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new BadRequestException("Código inválido ou expirado."));

        user.setSenhaHash(passwordEncoder.encode(request.getNovaSenha()));
        user.setAtualizadoEm(LocalDateTime.now());
        userRepository.save(user);

        log.info("Senha redefinida com sucesso para: {}", user.getEmail());

        return new MensagemResponse(
            "Senha redefinida com sucesso! Agora você já pode entrar com sua nova senha."
        );
    }

    private EmailVerificationCode validarEConsumirCodigo(
        String email,
        String codigoInformado,
        VerificationPurpose finalidade
    ) {
        EmailVerificationCode registro = verificationCodeRepository
            .findTopByEmailAndFinalidadeOrderByCriadoEmDesc(email, finalidade)
            .orElseThrow(() -> new BadRequestException(
                "Código inválido ou inexistente. Solicite um novo código."
            ));

        if (registro.getExpiraEm().isBefore(LocalDateTime.now())) {
            verificationAttemptService.remover(registro.getId());
            throw new BadRequestException("Este código expirou. Solicite um novo código.");
        }

        if (registro.getTentativas() >= MAX_TENTATIVAS_CODIGO) {
            verificationAttemptService.remover(registro.getId());
            throw new BadRequestException(
                "Limite de tentativas excedido. Solicite um novo código."
            );
        }

        if (!passwordEncoder.matches(codigoInformado, registro.getCodigoHash())) {
            int tentativas = verificationAttemptService.registrarFalha(
                registro.getId(),
                MAX_TENTATIVAS_CODIGO
            );
            if (tentativas >= MAX_TENTATIVAS_CODIGO) {
                throw new BadRequestException(
                    "Código incorreto. Limite de tentativas atingido — solicite um novo código."
                );
            }
            int restantes = MAX_TENTATIVAS_CODIGO - tentativas;
            throw new BadRequestException(
                "Código incorreto. Verifique o e-mail e tente novamente ("
                    + restantes + (restantes == 1 ? " tentativa restante)." : " tentativas restantes).")
            );
        }

        verificationCodeRepository.deleteByEmailAndFinalidade(email, finalidade);
        return registro;
    }

    private void verificarCooldown(String email, VerificationPurpose finalidade) {
        verificationCodeRepository.findTopByEmailAndFinalidadeOrderByCriadoEmDesc(email, finalidade)
            .ifPresent(this::verificarCooldownRegistro);
    }

    private void verificarCooldownRegistro(EmailVerificationCode registro) {
        long decorrido = Duration.between(registro.getCriadoEm(), LocalDateTime.now()).getSeconds();
        if (decorrido < COOLDOWN_REENVIO_SEGUNDOS) {
            long restante = COOLDOWN_REENVIO_SEGUNDOS - Math.max(0, decorrido);
            throw new TooManyRequestsException(
                "Aguarde " + restante + " segundos antes de solicitar um novo código.",
                restante
            );
        }
    }

    private String gerarCodigoSeisDigitos() {
        return String.format("%06d", secureRandom.nextInt(1_000_000));
    }

    private AuthResponse emitirSessao(User user, String token) {
        return responseMapper.toAuthResponse(
            user.getId(),
            token,
            jwtTokenProvider.getExpirationInSeconds(),
            user.getNome(),
            user.getEmail(),
            user.getFotoUrl(),
            user.getRole().name()
        );
    }
}
