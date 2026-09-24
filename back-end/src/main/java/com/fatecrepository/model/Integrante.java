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
@Table(name = "integrantes")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Integrante {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "projeto_id", nullable = false)
    @JsonIgnore
    @ToString.Exclude
    private Projeto projeto;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = true)
    private String linkLinkedin;

    @Column(nullable = false, updatable = false)
    private LocalDateTime criadoEm = LocalDateTime.now();
}
