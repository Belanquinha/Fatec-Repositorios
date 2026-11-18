package com.fatecrepository.service;

import com.fatecrepository.model.EmailVerificationCode;
import com.fatecrepository.repository.EmailVerificationCodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Persiste incrementos de tentativa incorreta e remoção de códigos inválidos em transação
 * independente ({@code REQUIRES_NEW}), garantindo que o lançamento de exceção no fluxo
 * principal não desfaça a contagem anti-brute-force.
 */
@Service
@RequiredArgsConstructor
public class VerificationAttemptService {

    private final EmailVerificationCodeRepository repository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int registrarFalha(UUID codigoId, int maxTentativas) {
        EmailVerificationCode registro = repository.findById(codigoId).orElse(null);
        if (registro == null) {
            return maxTentativas;
        }
        int novasTentativas = registro.getTentativas() + 1;
        if (novasTentativas >= maxTentativas) {
            repository.delete(registro);
        } else {
            registro.setTentativas(novasTentativas);
            repository.saveAndFlush(registro);
        }
        return novasTentativas;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void remover(UUID codigoId) {
        repository.deleteById(codigoId);
    }
}
