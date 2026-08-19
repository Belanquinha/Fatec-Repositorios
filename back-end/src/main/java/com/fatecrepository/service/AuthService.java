package com.fatecrepository.service;

import com.fatecrepository.dto.request.LoginRequest;
import com.fatecrepository.dto.response.AuthResponse;
import com.fatecrepository.exception.BadRequestException;
import com.fatecrepository.exception.UnauthorizedException;
import com.fatecrepository.mapper.ResponseMapper;
import com.fatecrepository.model.User;
import com.fatecrepository.model.UserRole;
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
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final ResponseMapper responseMapper;

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        log.info("Tentativa de login para email: {}", request.getEmail());

        User user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> new UnauthorizedException("Usuário não encontrado"));

        if (!passwordEncoder.matches(request.getSenha(), user.getSenha())) {
            throw new UnauthorizedException("Senha incorreta");
        }

        String token = jwtTokenProvider.generateToken(user);
        log.info("Login realizado com sucesso para: {}", request.getEmail());

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
}
