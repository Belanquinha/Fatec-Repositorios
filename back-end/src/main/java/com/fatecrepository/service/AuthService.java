package com.fatecrepository.service;

import com.fatecrepository.dto.request.LoginRequest;
import com.fatecrepository.dto.request.MicrosoftLoginRequest;
import com.fatecrepository.dto.response.AuthResponse;
import com.fatecrepository.exception.BadRequestException;
import com.fatecrepository.exception.UnauthorizedException;
import com.fatecrepository.mapper.ResponseMapper;
import com.fatecrepository.model.Gestor;
import com.fatecrepository.model.User;
import com.fatecrepository.model.UserRole;
import com.fatecrepository.repository.GestorRepository;
import com.fatecrepository.repository.UserRepository;
import com.fatecrepository.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final GestorRepository gestorRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final ResponseMapper responseMapper;
    private final MicrosoftGraphService microsoftGraphService;

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        log.info("Tentativa de login para email: {}", request.getEmail());

        Gestor gestor = gestorRepository.findByEmail(request.getEmail()).orElse(null);
        if (gestor != null) {
            if (!passwordEncoder.matches(request.getSenha(), gestor.getSenha())) {
                throw new UnauthorizedException("Senha incorreta");
            }

            String token = jwtTokenProvider.generateToken(gestor);
            log.info("Login realizado com sucesso para gestor: {}", request.getEmail());
            return responseMapper.toAuthResponse(token, jwtTokenProvider.getExpirationInSeconds());
        }

        User user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> new UnauthorizedException("Usuário não encontrado"));

        if (!passwordEncoder.matches(request.getSenha(), user.getSenha())) {
            throw new UnauthorizedException("Senha incorreta");
        }

        String token = jwtTokenProvider.generateToken(user);
        log.info("Login realizado com sucesso para usuário: {}", request.getEmail());

        return responseMapper.toAuthResponse(token, jwtTokenProvider.getExpirationInSeconds());
    }

    private void validarEmailDisponivel(String email) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new BadRequestException("Email já cadastrado");
        }
    }

    private User criarUsuarioBase(
        String nome,
        String email,
        String senha,
        UserRole role) {
        User user = new User();
        user.setNome(nome);
        user.setEmail(email);
        user.setSenha(passwordEncoder.encode(senha));
        user.setRole(role);
        user.setCriadoEm(LocalDateTime.now());
        user.setAtualizadoEm(LocalDateTime.now());
        return user;
    }

    @Transactional
    public AuthResponse loginMicrosoft(MicrosoftLoginRequest request) {
        log.info("Tentativa de login via Microsoft");

        MicrosoftGraphService.GraphUser graphUser = microsoftGraphService.getUserInfo(request.getAccessToken());
        if (graphUser == null || graphUser.getMail() == null) {
            throw new UnauthorizedException("Token Microsoft inválido ou dados do usuário não encontrados");
        }

        String email = graphUser.getMail();
        String nome = graphUser.getDisplayName();
        String fotoUrl = microsoftGraphService.getPhotoUrl(request.getAccessToken());

        User user = userRepository.findByEmail(email).orElseGet(() -> {
            log.info("Criando novo usuário Microsoft: {}", email);
            User novoUser = new User();
            novoUser.setNome(nome);
            novoUser.setEmail(email);
            novoUser.setRole(UserRole.GESTOR);
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
        log.info("Login Microsoft realizado com sucesso para: {}", email);

        return responseMapper.toAuthResponse(token, jwtTokenProvider.getExpirationInSeconds(), user.getNome(), user.getEmail(), user.getFotoUrl());
    }
}
