package com.fatecrepository.service;

import com.fatecrepository.dto.response.CursoResponse;
import com.fatecrepository.mapper.ResponseMapper;
import com.fatecrepository.repository.CursoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CursoService {

    private final CursoRepository cursoRepository;
    private final ResponseMapper responseMapper;

    public List<CursoResponse> obterTodosAtivos() {
        return cursoRepository.findByAtivoTrueOrderByNomeAsc().stream()
            .map(responseMapper::toCursoResponse)
            .toList();
    }
}
