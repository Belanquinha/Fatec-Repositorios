package com.fatecrepository.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "instituicoes")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Instituicao {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String codigoUnidade;

    @Column(nullable = false)
    private String nome;

    private String estado;

    @Column(nullable = false)
    private boolean ativo = true;

    private String endereco;
    private String cidade;
    private String regiaoAdministrativa;
    private String cnpj;
    private String telefone;
    private String site;
    private String linkLogo;

    @Column(nullable = false, updatable = false)
    private LocalDateTime criadoEm = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime atualizadoEm = LocalDateTime.now();
}