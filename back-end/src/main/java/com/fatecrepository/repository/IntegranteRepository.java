package com.fatecrepository.repository;

import com.fatecrepository.model.Integrante;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface IntegranteRepository extends JpaRepository<Integrante, UUID> {
    List<Integrante> findByProjetoId(UUID projetoId);
}
