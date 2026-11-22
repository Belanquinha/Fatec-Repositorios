package com.fatecrepository.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "usuarios")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String nome;

    // Nullable de propósito: a exclusão da conta (LGPD, SPEC-exclusao-projetos §4.6)
    // anonimiza a linha com `email = null`, o que invalida os tokens antigos — o filtro
    // JWT recarrega a conta por e-mail e passa a receber 401. Postgres aceita múltiplos
    // NULL no unique (NULLs não colidem entre si).
    @Column(nullable = true, unique = true)
    private String email;

    @JsonIgnore
    @ToString.Exclude
    @Column(name = "senha_hash", nullable = true)
    private String senhaHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @Column(nullable = true)
    private String fotoUrl;

    @Column(nullable = false, updatable = false)
    private LocalDateTime criadoEm = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime atualizadoEm = LocalDateTime.now();
}