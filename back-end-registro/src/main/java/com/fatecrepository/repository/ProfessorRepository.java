package com.fatecrepository.repository;

import com.fatecrepository.model.Professor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProfessorRepository extends JpaRepository<Professor, UUID> {
}
