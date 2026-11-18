package com.fatecrepository.repository;

import com.fatecrepository.model.EmailVerificationCode;
import com.fatecrepository.model.VerificationPurpose;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmailVerificationCodeRepository extends JpaRepository<EmailVerificationCode, UUID> {

    Optional<EmailVerificationCode> findTopByEmailAndFinalidadeOrderByCriadoEmDesc(
        String email,
        VerificationPurpose finalidade
    );

    void deleteByEmailAndFinalidade(String email, VerificationPurpose finalidade);
}
