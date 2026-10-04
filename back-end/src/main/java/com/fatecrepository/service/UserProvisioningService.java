package com.fatecrepository.service;

import com.fatecrepository.model.User;
import com.fatecrepository.model.UserRole;
import com.fatecrepository.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;

/**
 * Descobre ou cria o {@link User} a partir de um e-mail institucional, e decide o papel pelo
 * domínio desse e-mail.
 *
 * <p>Existe para que a classificação de papel tenha uma única implementação. O login via Microsoft
 * e o login de desenvolvimento precisam exatamente da mesma regra: duas cópias divergentes fariam o
 * mesmo endereço ser ALUNO em um caminho e PROFESSOR no outro, e essa inconsistência só apareceria
 * em produção.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserProvisioningService {

    private static final String DOMINIO_ALUNO = "@aluno.cps.sp.gov.br";
    private static final String DOMINIO_PROFESSOR = "@cps.sp.gov.br";

    private final UserRepository userRepository;

    /**
     * Devolve o usuário do e-mail, criando-o quando ainda não existe.
     *
     * <p>{@code fotoUrl} é aplicado apenas quando o usuário ainda não tem uma: um caminho que passe
     * {@code null} (o login de desenvolvimento) não pode apagar a foto que o Microsoft buscar.
     */
    @Transactional
    public User provisionar(String email, String nome, String fotoUrl) {
        String emailNormalizado = normalizarEmail(email);

        User user = userRepository.findByEmail(emailNormalizado).orElseGet(() -> {
            log.info("Criando novo usuário: {} (papel: {})", emailNormalizado, classificarPapel(emailNormalizado));
            User novoUser = new User();
            novoUser.setNome(nome);
            novoUser.setEmail(emailNormalizado);
            novoUser.setRole(classificarPapel(emailNormalizado));
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

        return user;
    }

    /**
     * O domínio do e-mail institucional é a lista de papéis (AD-2). O e-mail que o Microsoft devolveu
     * já vem normalizado por {@code resolverEmail}; normalizar de novo aqui mantém o método seguro
     * para quem chamar direto.
     *
     * <p>O fallback é ALUNO: um domínio desconhecido do tenant não deve ganhar privilégio de
     * professor, e professor é quem aprova projeto alheio.
     */
    public UserRole classificarPapel(String email) {
        String normalizado = normalizarEmail(email);

        if (normalizado.endsWith(DOMINIO_ALUNO)) {
            return UserRole.ALUNO;
        }
        if (normalizado.endsWith(DOMINIO_PROFESSOR)) {
            return UserRole.PROFESSOR;
        }
        return UserRole.ALUNO;
    }

    /**
     * A coluna de e-mail é única e a comparação de papel é por {@code endsWith}, então o valor
     * precisa estar trimmed e em minúsculas antes de qualquer um dos dois.
     */
    public String normalizarEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}