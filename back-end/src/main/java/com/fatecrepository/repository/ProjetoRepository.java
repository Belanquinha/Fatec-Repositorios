package com.fatecrepository.repository;

import com.fatecrepository.model.Projeto;
import com.fatecrepository.model.ProjetoEstado;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

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
    List<Projeto> findByAutorIdOrderByCriadoEmDesc(UUID autorId);

    @EntityGraph(attributePaths = {"instituicao", "curso", "autor"})
    List<Projeto> findByEstadoOrderByCriadoEmDesc(ProjetoEstado estado);

    @EntityGraph(attributePaths = {"instituicao", "curso", "autor"})
    List<Projeto> findByInstituicaoIdAndEstadoOrderByCriadoEmDesc(UUID instituicaoId, ProjetoEstado estado);

    @EntityGraph(attributePaths = {"instituicao", "curso", "autor"})
    List<Projeto> findByEstadoAndEmailProfessorResponsavelIgnoreCaseOrderByCriadoEmDesc(ProjetoEstado estado, String emailProfessorResponsavel);

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
