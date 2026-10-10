package com.fatecrepository.repository;

import com.fatecrepository.model.ProfessorInstituicao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ProfessorInstituicaoRepository extends JpaRepository<ProfessorInstituicao, UUID> {

    @Query("select v from ProfessorInstituicao v join fetch v.instituicao where v.usuario.id = :usuarioId")
    List<ProfessorInstituicao> buscarComInstituicao(@Param("usuarioId") UUID usuarioId);

    void deleteByUsuarioId(UUID usuarioId);

    boolean existsByUsuarioIdAndInstituicaoId(UUID usuarioId, UUID instituicaoId);
}
