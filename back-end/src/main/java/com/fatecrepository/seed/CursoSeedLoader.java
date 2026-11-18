package com.fatecrepository.seed;

import com.fatecrepository.model.Curso;
import com.fatecrepository.model.Instituicao;
import com.fatecrepository.model.InstituicaoCurso;
import com.fatecrepository.repository.CursoRepository;
import com.fatecrepository.repository.InstituicaoCursoRepository;
import com.fatecrepository.repository.InstituicaoRepository;
import com.fatecrepository.repository.ProjetoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CursoSeedLoader {

    private final CursoRepository cursoRepository;
    private final InstituicaoRepository instituicaoRepository;
    private final InstituicaoCursoRepository instituicaoCursoRepository;
    private final ProjetoRepository projetoRepository;

    @Transactional
    public int upsert(List<CursoCsvParser.VinculoCursoSeed> vinculos) {
        Map<String, Instituicao> instituicoesPorCodigo = instituicaoRepository.findAll().stream()
            .collect(Collectors.toMap(Instituicao::getCodigoUnidade, Function.identity(), (a, b) -> a));

        Map<String, Curso> cursosPorNome = new HashMap<>();
        for (Curso c : cursoRepository.findAll()) {
            cursosPorNome.put(c.getNome().toLowerCase(Locale.ROOT), c);
        }

        int vinculosInseridos = 0;
        int vinculosExistentes = 0;
        int cursosCriados = 0;

        for (CursoCsvParser.VinculoCursoSeed item : vinculos) {
            Instituicao instituicao = instituicoesPorCodigo.get(item.codigoUnidade());
            if (instituicao == null) {
                log.warn("[seed-cursos] instituição código '{}' não encontrada para o curso '{}', ignorando",
                    item.codigoUnidade(), item.nomeCurso());
                continue;
            }

            String chaveNome = item.nomeCurso().toLowerCase(Locale.ROOT);
            Curso curso = cursosPorNome.get(chaveNome);
            if (curso == null) {
                Curso novo = new Curso();
                novo.setNome(item.nomeCurso());
                novo.setAtivo(true);
                LocalDateTime agora = LocalDateTime.now();
                novo.setCriadoEm(agora);
                novo.setAtualizadoEm(agora);
                curso = cursoRepository.save(novo);
                cursosPorNome.put(chaveNome, curso);
                cursosCriados++;
            }

            if (instituicaoCursoRepository.existsByInstituicaoIdAndCursoId(instituicao.getId(), curso.getId())) {
                vinculosExistentes++;
            } else {
                InstituicaoCurso vinculo = new InstituicaoCurso();
                vinculo.setInstituicao(instituicao);
                vinculo.setCurso(curso);
                instituicaoCursoRepository.save(vinculo);
                vinculosInseridos++;
            }
        }

        log.info("[seed-cursos] upsert concluído: {} cursos novos, {} vínculos novos, {} vínculos já existentes",
            cursosCriados, vinculosInseridos, vinculosExistentes);
        return vinculosInseridos;
    }

    @Transactional
    public int replace(List<CursoCsvParser.VinculoCursoSeed> vinculos) {
        instituicaoCursoRepository.deleteAll();
        instituicaoCursoRepository.flush();
        if (projetoRepository.count() == 0) {
            cursoRepository.deleteAll();
            cursoRepository.flush();
        }
        return upsert(vinculos);
    }
}
