package com.fatecrepository.repository;

import com.fatecrepository.model.Projeto;
import com.fatecrepository.model.ProjetoEstado;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProjetoRepository extends JpaRepository<Projeto, UUID> {
    List<Projeto> findByAutorIdOrderByCriadoEmDesc(UUID autorId);
    List<Projeto> findByEstadoOrderByCriadoEmDesc(ProjetoEstado estado);
    List<Projeto> findByInstituicaoIdAndEstadoOrderByCriadoEmDesc(UUID instituicaoId, ProjetoEstado estado);
}
