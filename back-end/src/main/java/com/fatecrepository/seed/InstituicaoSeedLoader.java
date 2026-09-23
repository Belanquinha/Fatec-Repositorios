package com.fatecrepository.seed;

import com.fatecrepository.model.Instituicao;
import com.fatecrepository.repository.InstituicaoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class InstituicaoSeedLoader {

    private final InstituicaoRepository instituicaoRepository;

    @Transactional
    public int upsert(List<Instituicao> instituicoes) {
        int inseridas = 0;
        int existentes = 0;
        for (Instituicao instituicao : instituicoes) {
            if (instituicaoRepository.existsByCodigoUnidade(instituicao.getCodigoUnidade())) {
                existentes++;
            } else {
                instituicaoRepository.save(instituicao);
                inseridas++;
            }
        }
        log.info("[seed] upsert concluído: {} inseridas, {} já existentes (inalteradas)", inseridas, existentes);
        return inseridas;
    }

    @Transactional
    public int replace(List<Instituicao> instituicoes) {
        instituicaoRepository.deleteAll();
        instituicaoRepository.flush();
        List<Instituicao> salvas = instituicaoRepository.saveAll(instituicoes);
        log.info("[seed] replace concluído: {} instituições reinseridas a partir da fonte", salvas.size());
        return salvas.size();
    }
}