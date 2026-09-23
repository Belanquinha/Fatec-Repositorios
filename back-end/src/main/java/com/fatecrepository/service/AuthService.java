package com.fatecrepository.service;

import com.fatecrepository.dto.request.MicrosoftLoginRequest;
import com.fatecrepository.dto.response.AuthResponse;
import com.fatecrepository.exception.UnauthorizedException;
import com.fatecrepository.mapper.ResponseMapper;
import com.fatecrepository.model.User;
import com.fatecrepository.model.UserRole;
import com.fatecrepository.repository.UserRepository;
import com.fatecrepository.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class AuthService {

    private static final String DOMINIO_ALUNO = "@aluno.cps.sp.gov.br";
    private static final String DOMINIO_PROFESSOR = "@cps.sp.gov.br";

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final ResponseMapper responseMapper;
    private final MicrosoftGraphService microsoftGraphService;

    @Transactional
    public AuthResponse loginMicrosoft(MicrosoftLoginRequest request) {
        log.info("Tentativa de login via Microsoft");

        MicrosoftGraphService.GraphUser graphUser = microsoftGraphService.getUserInfo(request.getAccessToken());
        if (graphUser == null || graphUser.getMail() == null) {
            throw new UnauthorizedException("Token Microsoft inválido ou dados do usuário não encontrados");
        }

        String email = graphUser.getMail().toLowerCase();
        String nome = graphUser.getDisplayName();
        String fotoUrl = microsoftGraphService.getPhotoUrl(request.getAccessToken());

        User user = userRepository.findByEmail(email).orElseGet(() -> {
            log.info("Criando novo usuário Microsoft: {}", email);
            User novoUser = new User();
            novoUser.setNome(nome);
            novoUser.setEmail(email);
            novoUser.setRole(classificarPapel(email));
            novoUser.setFotoUrl(fotoUrl);
            novoUser.setCriadoEm(LocalDateTime.now());
            novoUser.setAtualizadoEm(LocalDateTime.now());
            return userRepository.save(novoUser);
        });

        if (user.getFotoUrl() == null && fotoUrl != null) {
            user.setFotoUrl(fotoUrl);
            user.setAtualizadoEm(LocalDateTime.now());
            userRepository.save(user);
        }

        String token = jwtTokenProvider.generateToken(user);
        log.info("Login Microsoft realizado com sucesso para: {} (papel: {})", email, user.getRole().name());

        return responseMapper.toAuthResponse(token, jwtTokenProvider.getExpirationInSeconds(), user.getNome(), user.getEmail(), user.getFotoUrl(), user.getRole().name());
    }

    private UserRole classificarPapel(String email) {
        if (email.endsWith(DOMINIO_ALUNO)) {
            return UserRole.ALUNO;
        }
        if (email.endsWith(DOMINIO_PROFESSOR)) {
            return UserRole.PROFESSOR;
        }
        return UserRole.ALUNO;
    }
}