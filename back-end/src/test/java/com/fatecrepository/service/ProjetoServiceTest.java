package com.fatecrepository.service;

import com.fatecrepository.dto.request.IntegranteRequest;
import com.fatecrepository.dto.request.ProjetoCreateRequest;
import com.fatecrepository.dto.response.ProjetoResponse;
import com.fatecrepository.exception.BadRequestException;
import com.fatecrepository.exception.ResourceNotFoundException;
import com.fatecrepository.mapper.ResponseMapper;
import com.fatecrepository.model.*;
import com.fatecrepository.repository.InstituicaoRepository;
import com.fatecrepository.repository.ProjetoRepository;
import com.fatecrepository.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjetoServiceTest {

    @Mock
    private ProjetoRepository projetoRepository;

    @Mock
    private InstituicaoRepository instituicaoRepository;

    @Mock
    private UserRepository userRepository;

    @Spy
    private ResponseMapper responseMapper = new ResponseMapper();

    @InjectMocks
    private ProjetoService projetoService;

    private User autor;
    private Instituicao instituicao;

    @BeforeEach
    void setUp() {
        autor = new User();
        autor.setId(UUID.randomUUID());
        autor.setNome("Aluno Gabriel");
        autor.setEmail("gabriel@aluno.cps.sp.gov.br");
        autor.setRole(UserRole.ALUNO);

        instituicao = new Instituicao();
        instituicao.setId(UUID.randomUUID());
        instituicao.setNome("Fatec Ipiranga");
        instituicao.setCodigoUnidade("001");
        instituicao.setAtivo(true);
    }

    @Test
    @DisplayName("Deve criar projeto com sucesso no estado AGUARDANDO_APROVACAO (Regra AD-7)")
    void deveCriarProjetoComSucessoNoEstadoAguardandoAprovacao() {
        UUID autorId = autor.getId();
        UUID instituicaoId = instituicao.getId();

        ProjetoCreateRequest request = new ProjetoCreateRequest();
        request.setTitulo("Plataforma de Repositório Fatec");
        request.setDescricaoCurta("Sistema para catalogar TCCs e projetos integradores.");
        request.setConteudoEditorJs("{\"blocks\":[]}");
        request.setLinkRepositorio("https://github.com/fatec/repo");
        request.setImagemCapaUrl("/uploads/capa.jpg");
        request.setPalavrasChave(List.of("Java", "Angular", "TCC"));
        request.setInstituicaoId(instituicaoId);
        request.setEmailProfessorResponsavel("PROFESSOR@CPS.SP.GOV.BR");
        request.setIntegrantes(List.of(
            new IntegranteRequest("Gabriel Belan", "https://linkedin.com/in/gabriel"),
            new IntegranteRequest("Maria Silva", "https://linkedin.com/in/maria")
        ));

        when(userRepository.findById(autorId)).thenReturn(Optional.of(autor));
        when(instituicaoRepository.findById(instituicaoId)).thenReturn(Optional.of(instituicao));
        when(projetoRepository.save(any(Projeto.class))).thenAnswer(invocation -> {
            Projeto p = invocation.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });

        ProjetoResponse response = projetoService.criar(request, autorId);

        assertNotNull(response);
        assertNotNull(response.getId());
        assertEquals("Plataforma de Repositório Fatec", response.getTitulo());
        assertEquals("AGUARDANDO_APROVACAO", response.getEstado());
        assertEquals("professor@cps.sp.gov.br", response.getEmailProfessorResponsavel());
        assertEquals(2, response.getIntegrantes().size());
        assertEquals("Fatec Ipiranga", response.getInstituicaoNome());
        assertEquals("Aluno Gabriel", response.getAutorNome());

        verify(projetoRepository, times(1)).save(any(Projeto.class));
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException quando instituição não existir")
    void deveLancarExcecaoQuandoInstituicaoNaoExistir() {
        UUID autorId = autor.getId();
        UUID instituicaoId = UUID.randomUUID();

        ProjetoCreateRequest request = new ProjetoCreateRequest();
        request.setTitulo("Projeto Teste");
        request.setDescricaoCurta("Desc");
        request.setInstituicaoId(instituicaoId);
        request.setEmailProfessorResponsavel("prof@cps.sp.gov.br");
        request.setIntegrantes(List.of(new IntegranteRequest("Aluno", null)));

        when(userRepository.findById(autorId)).thenReturn(Optional.of(autor));
        when(instituicaoRepository.findById(instituicaoId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> projetoService.criar(request, autorId));
        verify(projetoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar BadRequestException quando instituição estiver inativa")
    void deveLancarExcecaoQuandoInstituicaoInativa() {
        UUID autorId = autor.getId();
        UUID instituicaoId = instituicao.getId();
        instituicao.setAtivo(false);

        ProjetoCreateRequest request = new ProjetoCreateRequest();
        request.setTitulo("Projeto Teste");
        request.setDescricaoCurta("Desc");
        request.setInstituicaoId(instituicaoId);
        request.setEmailProfessorResponsavel("prof@cps.sp.gov.br");
        request.setIntegrantes(List.of(new IntegranteRequest("Aluno", null)));

        when(userRepository.findById(autorId)).thenReturn(Optional.of(autor));
        when(instituicaoRepository.findById(instituicaoId)).thenReturn(Optional.of(instituicao));

        assertThrows(BadRequestException.class, () -> projetoService.criar(request, autorId));
        verify(projetoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar BadRequestException quando nenhum integrante for fornecido")
    void deveLancarExcecaoQuandoNenhumIntegranteFornecido() {
        UUID autorId = autor.getId();
        UUID instituicaoId = instituicao.getId();

        ProjetoCreateRequest request = new ProjetoCreateRequest();
        request.setTitulo("Projeto Teste");
        request.setDescricaoCurta("Desc");
        request.setInstituicaoId(instituicaoId);
        request.setEmailProfessorResponsavel("prof@cps.sp.gov.br");
        request.setIntegrantes(Collections.emptyList());

        when(userRepository.findById(autorId)).thenReturn(Optional.of(autor));
        when(instituicaoRepository.findById(instituicaoId)).thenReturn(Optional.of(instituicao));

        assertThrows(BadRequestException.class, () -> projetoService.criar(request, autorId));
        verify(projetoRepository, never()).save(any());
    }
}
