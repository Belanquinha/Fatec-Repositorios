package com.fatecrepository.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.BatchSize;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "projetos")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Projeto {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String titulo;

    @Column(nullable = true, length = 144)
    private String descricaoCurta;

    @Column(columnDefinition = "TEXT")
    private String conteudoEditorJs;

    @Column(nullable = true, length = 500)
    private String linkRepositorio;

    @Column(nullable = true, length = 500)
    private String imagemCapaUrl;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "projeto_palavras_chave", joinColumns = @JoinColumn(name = "projeto_id"))
    @Column(name = "palavra_chave")
    private List<String> palavrasChave = new ArrayList<>();

    @Column(nullable = false)
    private Integer anoPublicado;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProjetoEstado estado = ProjetoEstado.AGUARDANDO_APROVACAO;

    @Column(nullable = true, length = 2000, columnDefinition = "TEXT")
    private String motivoRejeicao;

    @Column(nullable = true)
    private LocalDateTime removidoEm;

    @Column(nullable = true)
    private UUID removidoPorId;

    @Column(nullable = true, length = 40)
    private String motivoRemocaoGenerico;

    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    private ProjetoEstado estadoAnterior;

    @Column(nullable = true, columnDefinition = "TEXT")
    private String dadosOriginais;

    @Column(nullable = true)
    private String emailProfessorResponsavel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instituicao_id", nullable = false)
    @ToString.Exclude
    private Instituicao instituicao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "curso_id", nullable = false)
    @ToString.Exclude
    private Curso curso;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    @ToString.Exclude
    private User autor;

    @OneToMany(mappedBy = "projeto", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @BatchSize(size = 32)
    private List<Integrante> integrantes = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private LocalDateTime criadoEm = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime atualizadoEm = LocalDateTime.now();

    /**
     * Lock otimista: duas avaliações concorrentes (aprovar × rejeitar) sobre o mesmo
     * projeto fazem o perdedor falhar com OptimisticLock → 409 (via
     * ConcurrencyFailureException), em vez de last-write-wins silencioso.
     * O default 0 protege linhas legadas criadas antes da coluna existir.
     */
    @Version
    @Column(nullable = false, columnDefinition = "bigint default 0")
    private Long versao = 0L;

    public void adicionarIntegrante(Integrante integrante) {
        integrantes.add(integrante);
        integrante.setProjeto(this);
    }

    public void removerIntegrante(Integrante integrante) {
        integrantes.remove(integrante);
        integrante.setProjeto(null);
    }
}
