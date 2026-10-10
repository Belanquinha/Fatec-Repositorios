package com.fatecrepository.repository;

import com.fatecrepository.model.Projeto;
import com.fatecrepository.model.ProjetoEstado;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProjetoRepository extends JpaRepository<Projeto, UUID> {
    // Só single-valued no graph: incluir as duas coleções List (bags) junto com a
    // EAGER palavrasChave estouraria MultipleBagFetchException em runtime.
    // As coleções são cobertas por @BatchSize no modelo (1 select em lote, sem N+1).
    @EntityGraph(attributePaths = {"instituicao", "autor"})
    List<Projeto> findByAutorIdOrderByCriadoEmDesc(UUID autorId);
    @EntityGraph(attributePaths = {"instituicao", "autor"})
    List<Projeto> findByEstadoOrderByCriadoEmDesc(ProjetoEstado estado);
    List<Projeto> findByInstituicaoIdAndEstadoOrderByCriadoEmDesc(UUID instituicaoId, ProjetoEstado estado);
    @EntityGraph(attributePaths = {"instituicao", "autor"})
    List<Projeto> findByEstadoAndEmailProfessorResponsavelIgnoreCaseOrderByCriadoEmDesc(ProjetoEstado estado, String emailProfessorResponsavel);
    boolean existsByTitulo(String titulo);
    java.util.Optional<Projeto> findByTitulo(String titulo);
}
