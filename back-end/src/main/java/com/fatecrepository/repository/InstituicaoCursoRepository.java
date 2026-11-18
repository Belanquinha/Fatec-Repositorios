package com.fatecrepository.repository;

import com.fatecrepository.model.Curso;
import com.fatecrepository.model.InstituicaoCurso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface InstituicaoCursoRepository extends JpaRepository<InstituicaoCurso, UUID> {

    @Query("select ic.curso from InstituicaoCurso ic where ic.instituicao.id = :instituicaoId and ic.curso.ativo = true order by ic.curso.nome asc")
    List<Curso> buscarCursosAtivosPorInstituicao(@Param("instituicaoId") UUID instituicaoId);

    @Query("select ic from InstituicaoCurso ic join fetch ic.curso where ic.instituicao.id in :instituicaoIds and ic.curso.ativo = true")
    List<InstituicaoCurso> buscarAtivosPorInstituicoes(@Param("instituicaoIds") Collection<UUID> instituicaoIds);

    boolean existsByInstituicaoIdAndCursoId(UUID instituicaoId, UUID cursoId);

    void deleteByInstituicaoId(UUID instituicaoId);
}
