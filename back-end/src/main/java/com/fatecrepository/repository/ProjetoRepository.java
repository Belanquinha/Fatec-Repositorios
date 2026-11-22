package com.fatecrepository.repository;

import com.fatecrepository.model.Integrante;
import com.fatecrepository.model.Projeto;
import com.fatecrepository.model.ProjetoEstado;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjetoRepository extends JpaRepository<Projeto, UUID> {
    // Só single-valued no graph: incluir as duas coleções List (bags) junto com a
    // EAGER palavrasChave estouraria MultipleBagFetchException em runtime.
    // As coleções são cobertas por @BatchSize no modelo (1 select em lote, sem N+1).
    @Override
    @EntityGraph(attributePaths = {"instituicao", "curso", "autor"})
    Optional<Projeto> findById(UUID id);

    @EntityGraph(attributePaths = {"instituicao", "curso", "autor"})
    List<Projeto> findByAutorIdAndEstadoNotOrderByCriadoEmDesc(UUID autorId, ProjetoEstado estado);

    @EntityGraph(attributePaths = {"instituicao", "curso", "autor"})
    List<Projeto> findByAutorIdOrderByCriadoEmDesc(UUID autorId);

    @EntityGraph(attributePaths = {"instituicao", "curso", "autor"})
    List<Projeto> findByEstadoOrderByCriadoEmDesc(ProjetoEstado estado);

    @EntityGraph(attributePaths = {"instituicao", "curso", "autor"})
    List<Projeto> findByInstituicaoIdAndEstadoOrderByCriadoEmDesc(UUID instituicaoId, ProjetoEstado estado);

    @EntityGraph(attributePaths = {"instituicao", "curso", "autor"})
    List<Projeto> findByEstadoAndEmailProfessorResponsavelIgnoreCaseOrderByCriadoEmDesc(ProjetoEstado estado, String emailProfessorResponsavel);

    /** Fila do professor: AGUARDANDO_APROVACAO + REENVIADO (SPEC-exclusao-projetos §4.2). */
    @EntityGraph(attributePaths = {"instituicao", "curso", "autor"})
    List<Projeto> findByEstadoInAndEmailProfessorResponsavelIgnoreCaseOrderByCriadoEmDesc(
        List<ProjetoEstado> estados, String emailProfessorResponsavel);

    /** Pendentes em que o e-mail é o responsável — usado pela exclusão de conta. */
    @EntityGraph(attributePaths = {"instituicao", "curso", "autor"})
    List<Projeto> findByEmailProfessorResponsavelIgnoreCaseAndEstadoIn(
        String emailProfessorResponsavel, List<ProjetoEstado> estados);

    @EntityGraph(attributePaths = {"instituicao", "curso", "autor"})
    List<Projeto> findByAutorIdAndEstadoOrderByRemovidoEmDesc(UUID autorId, ProjetoEstado estado);

    @EntityGraph(attributePaths = {"instituicao", "curso", "autor"})
    List<Projeto> findByEstadoAndRemovidoEmBefore(ProjetoEstado estado, java.time.LocalDateTime limite);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM Projeto p WHERE p.id = :id")
    int excluirPorId(@Param("id") UUID id);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM Integrante i WHERE i.id = :id")
    int excluirIntegrantePorId(@Param("id") UUID id);

    /**
     * Filhos por pai: usada pela exclusão de conta, onde o lote vem de uma lista
     * já carregada e acessar a coleção lazy de cada linha detonaria
     * LazyInitializationException depois do primeiro clearAutomatically.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM Integrante i WHERE i.projeto.id = :projetoId")
    int excluirIntegrantesPorProjeto(@Param("projetoId") UUID projetoId);

    @EntityGraph(attributePaths = {"instituicao", "curso", "autor"})
    @org.springframework.data.jpa.repository.Query(
        "SELECT DISTINCT p FROM Projeto p " +
        "WHERE p.estado = :estado " +
        "AND (p.autor.id = :usuarioId OR LOWER(p.emailProfessorResponsavel) = LOWER(:email)) " +
        "ORDER BY p.criadoEm DESC"
    )
    List<Projeto> findProjetosAprovadosPorUsuario(
        @org.springframework.data.repository.query.Param("usuarioId") UUID usuarioId,
        @org.springframework.data.repository.query.Param("email") String email,
        @org.springframework.data.repository.query.Param("estado") ProjetoEstado estado
    );

    boolean existsByTitulo(String titulo);

    Optional<Projeto> findByTitulo(String titulo);
}
