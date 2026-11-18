package com.fatecrepository.service;

import com.fatecrepository.dto.request.InstituicaoRequest;
import com.fatecrepository.dto.response.CursoResponse;
import com.fatecrepository.dto.response.InstituicaoResponse;
import com.fatecrepository.dto.response.PerfilInstituicaoResponse;
import com.fatecrepository.dto.response.ProjetoResponse;
import com.fatecrepository.exception.BadRequestException;
import com.fatecrepository.exception.ResourceNotFoundException;
import com.fatecrepository.mapper.ResponseMapper;
import com.fatecrepository.model.Instituicao;
import com.fatecrepository.model.Projeto;
import com.fatecrepository.model.ProjetoEstado;
import com.fatecrepository.model.UserRole;
import com.fatecrepository.repository.InstituicaoCursoRepository;
import com.fatecrepository.repository.InstituicaoRepository;
import com.fatecrepository.repository.ProjetoRepository;
import com.fatecrepository.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class InstituicaoService {

    private final InstituicaoRepository instituicaoRepository;
    private final InstituicaoCursoRepository instituicaoCursoRepository;
    private final ProjetoRepository projetoRepository;
    private final UserRepository userRepository;
    private final ResponseMapper responseMapper;

    @Transactional
    public InstituicaoResponse criar(InstituicaoRequest request) {
        log.info("Criando nova instituição: {}", request.getNome());

        if (instituicaoRepository.existsByCodigoUnidade(request.getCodigoUnidade())) {
            throw new BadRequestException("Já existe instituição com o código da unidade " + request.getCodigoUnidade());
        }

        Instituicao instituicao = new Instituicao();
        aplicarDados(instituicao, request);
        instituicao.setCriadoEm(LocalDateTime.now());
        instituicao.setAtualizadoEm(LocalDateTime.now());

        Instituicao salva = instituicaoRepository.save(instituicao);
        log.info("Instituição criada com sucesso: {} (código {})", salva.getId(), salva.getCodigoUnidade());

        return responseMapper.toInstituicaoResponse(salva);
    }

    public InstituicaoResponse obterPorId(UUID id) {
        Instituicao instituicao = instituicaoRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Instituição não encontrada"));

        return responseMapper.toInstituicaoResponse(instituicao);
    }

    public List<InstituicaoResponse> obterTodas() {
        return instituicaoRepository.findAll().stream()
            .map(responseMapper::toInstituicaoResponse)
            .collect(Collectors.toList());
    }

    public List<CursoResponse> obterCursosDaInstituicao(UUID instituicaoId) {
        if (!instituicaoRepository.existsById(instituicaoId)) {
            throw new ResourceNotFoundException("Instituição não encontrada");
        }
        return instituicaoCursoRepository.buscarCursosAtivosPorInstituicao(instituicaoId).stream()
            .map(responseMapper::toCursoResponse)
            .toList();
    }

    public PerfilInstituicaoResponse obterPerfil(UUID instituicaoId) {
        Instituicao instituicao = instituicaoRepository.findById(instituicaoId)
            .orElseThrow(() -> new ResourceNotFoundException("Instituição não encontrada"));

        List<CursoResponse> cursos = instituicaoCursoRepository.buscarCursosAtivosPorInstituicao(instituicaoId).stream()
            .map(responseMapper::toCursoResponse)
            .toList();

        List<Projeto> projetosEntidades = projetoRepository
            .findByInstituicaoIdAndEstadoOrderByCriadoEmDesc(instituicaoId, ProjetoEstado.APROVADO);

        List<ProjetoResponse> projetos = projetosEntidades.stream()
            .map(p -> {
                ProjetoResponse resp = responseMapper.toProjetoResponse(p);
                enriquecerESanitizarProjeto(resp);
                return resp;
            })
            .toList();

        return PerfilInstituicaoResponse.builder()
            .id(instituicao.getId())
            .codigoUnidade(instituicao.getCodigoUnidade())
            .nome(instituicao.getNome())
            .endereco(instituicao.getEndereco())
            .cidade(instituicao.getCidade())
            .estado(instituicao.getEstado())
            .regiaoAdministrativa(instituicao.getRegiaoAdministrativa())
            .telefone(instituicao.getTelefone())
            .site(instituicao.getSite())
            .linkLogo(instituicao.getLinkLogo())
            .ativo(instituicao.isAtivo())
            .cursos(cursos)
            .projetos(projetos)
            .build();
    }

    private void enriquecerESanitizarProjeto(ProjetoResponse resp) {
        if (resp == null) return;
        if (resp.getEmailProfessorResponsavel() != null) {
            userRepository.findByEmail(resp.getEmailProfessorResponsavel())
                .ifPresent(u -> {
                    resp.setNomeProfessorResponsavel(u.getNome());
                    if (u.getRole() == UserRole.PROFESSOR) {
                        resp.setProfessorResponsavelId(u.getId());
                    }
                });
        }
        resp.setEmailProfessorResponsavel(null);
        resp.setMotivoRejeicao(null);
    }

    @Transactional
    public InstituicaoResponse atualizar(UUID id, InstituicaoRequest request) {
        log.info("Atualizando instituição: {}", id);

        Instituicao instituicao = instituicaoRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Instituição não encontrada"));

        if (instituicaoRepository.findByCodigoUnidade(request.getCodigoUnidade())
            .filter(existente -> !existente.getId().equals(id))
            .isPresent()) {
            throw new BadRequestException("Já existe instituição com o código da unidade " + request.getCodigoUnidade());
        }

        aplicarDados(instituicao, request);
        instituicao.setAtualizadoEm(LocalDateTime.now());

        Instituicao atualizada = instituicaoRepository.save(instituicao);
        log.info("Instituição atualizada com sucesso: {}", id);

        return responseMapper.toInstituicaoResponse(atualizada);
    }

    @Transactional
    public void deletar(UUID id) {
        log.info("Deletando instituição: {}", id);

        instituicaoRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Instituição não encontrada"));

        instituicaoCursoRepository.deleteByInstituicaoId(id);
        instituicaoRepository.deleteById(id);
        log.info("Instituição deletada com sucesso: {}", id);
    }

    private void aplicarDados(Instituicao instituicao, InstituicaoRequest request) {
        instituicao.setCodigoUnidade(request.getCodigoUnidade().trim());
        instituicao.setNome(request.getNome().trim());
        instituicao.setEndereco(trimToNull(request.getEndereco()));
        instituicao.setCidade(trimToNull(request.getCidade()));
        instituicao.setEstado(request.getEstado() == null || request.getEstado().isBlank() ? "SP" : request.getEstado().trim());
        instituicao.setRegiaoAdministrativa(trimToNull(request.getRegiaoAdministrativa()));
        instituicao.setCnpj(trimToNull(request.getCnpj()));
        instituicao.setTelefone(trimToNull(request.getTelefone()));
        instituicao.setSite(trimToNull(request.getSite()));
        instituicao.setLinkLogo(trimToNull(request.getLinkLogo()));
        if (request.getAtivo() != null) {
            instituicao.setAtivo(request.getAtivo());
        }
    }

    private String trimToNull(String valor) {
        return valor == null ? null : valor.trim().isEmpty() ? null : valor.trim();
    }
}
