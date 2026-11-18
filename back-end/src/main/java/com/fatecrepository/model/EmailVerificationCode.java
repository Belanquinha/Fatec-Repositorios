package com.fatecrepository.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "codigos_verificacao_email",
    indexes = {
        @Index(name = "idx_codigos_verificacao_email_finalidade", columnList = "email, finalidade")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmailVerificationCode {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private VerificationPurpose finalidade;

    @Column(nullable = true)
    private String nome;

    @ToString.Exclude
    @Column(name = "senha_hash", nullable = true)
    private String senhaHash;

    @ToString.Exclude
    @Column(name = "codigo_hash", nullable = false)
    private String codigoHash;

    @Column(nullable = false)
    private int tentativas = 0;

    @Column(name = "expira_em", nullable = false)
    private LocalDateTime expiraEm;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm = LocalDateTime.now();
}
