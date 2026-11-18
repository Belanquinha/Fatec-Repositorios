package com.fatecrepository.service;

import com.fatecrepository.dto.response.PerfilPublicoUsuarioResponse;
import com.fatecrepository.exception.ResourceNotFoundException;
import com.fatecrepository.mapper.ResponseMapper;
import com.fatecrepository.model.*;
import com.fatecrepository.repository.ProfessorInstituicaoRepository;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProfessorInstituicaoRepository professorInstituicaoRepository;

    @Mock
    private ProjetoRepository projetoRepository;

    @Spy
    private ResponseMapper responseMapper = new ResponseMapper();

    @InjectMocks
    private UsuarioService usuarioService;

    private User aluno;
    private User professor;
    private Instituicao fatec;
    private Curso curso;
    private Projeto projetoAprovado;

    @BeforeEach
    void setUp() {
        aluno = new User();
        aluno.setId(UUID.randomUUID());
        aluno.setNome("Aluno Teste");
        aluno.setEmail("aluno@fatec.sp.gov.br");
        aluno.setRole(UserRole.ALUNO);

        professor = new User();
        professor.setId(UUID.randomUUID());
        professor.setNome("Prof. Teste");
        professor.setEmail("prof@fatec.sp.gov.br");
        professor.setRole(UserRole.PROFESSOR);

        fatec = new Instituicao();
        fatec.setId(UUID.randomUUID());
        fatec.setNome("Fatec São Paulo");
        fatec.setCodigoUnidade("001");
        fatec.setAtivo(true);

        curso = new Curso();
        curso.setId(UUID.randomUUID());
        curso.setNome("Análise e Desenvolvimento de Sistemas");

        projetoAprovado = new Projeto();
        projetoAprovado.setId(UUID.randomUUID());
        projetoAprovado.setTitulo("Projeto Aprovado");
        projetoAprovado.setEstado(ProjetoEstado.APROVADO);
        projetoAprovado.setAutor(aluno);
        projetoAprovado.setInstituicao(fatec);
        projetoAprovado.setCurso(curso);
        projetoAprovado.setEmailProfessorResponsavel(professor.getEmail());
    }

    @Test
    @DisplayName("Deve retornar perfil público de aluno sem e-mail e apenas com projetos aprovados")
    void deveRetornarPerfilPublicoDeAluno() {
        when(userRepository.findById(aluno.getId())).thenReturn(Optional.of(aluno));
        when(projetoRepository.findProjetosAprovadosPorUsuario(aluno.getId(), aluno.getEmail(), ProjetoEstado.APROVADO))
            .thenReturn(List.of(projetoAprovado));
        when(userRepository.findByEmail(professor.getEmail())).thenReturn(Optional.of(professor));

        PerfilPublicoUsuarioResponse perfil = usuarioService.obterPerfilPublico(aluno.getId());

        assertNotNull(perfil);
        assertEquals(aluno.getId(), perfil.getId());
        assertEquals("Aluno Teste", perfil.getNome());
        assertEquals(UserRole.ALUNO, perfil.getRole());
        assertTrue(perfil.getInstituicoes().isEmpty());
        assertEquals(1, perfil.getProjetos().size());

        // Valida LGPD: sem e-mail do professor responsável
        assertNull(perfil.getProjetos().get(0).getEmailProfessorResponsavel());
        assertEquals("Prof. Teste", perfil.getProjetos().get(0).getNomeProfessorResponsavel());
        assertEquals(professor.getId(), perfil.getProjetos().get(0).getProfessorResponsavelId());
    }

    @Test
    @DisplayName("Deve retornar perfil público de professor com instituições vinculadas")
    void deveRetornarPerfilPublicoDeProfessor() {
        when(userRepository.findById(professor.getId())).thenReturn(Optional.of(professor));

        ProfessorInstituicao vinculo = new ProfessorInstituicao();
        vinculo.setId(UUID.randomUUID());
        vinculo.setUsuario(professor);
        vinculo.setInstituicao(fatec);

        when(professorInstituicaoRepository.buscarComInstituicao(professor.getId()))
            .thenReturn(List.of(vinculo));
        when(projetoRepository.findProjetosAprovadosPorUsuario(professor.getId(), professor.getEmail(), ProjetoEstado.APROVADO))
            .thenReturn(List.of(projetoAprovado));
        when(userRepository.findByEmail(professor.getEmail())).thenReturn(Optional.of(professor));

        PerfilPublicoUsuarioResponse perfil = usuarioService.obterPerfilPublico(professor.getId());

        assertNotNull(perfil);
        assertEquals(professor.getId(), perfil.getId());
        assertEquals(UserRole.PROFESSOR, perfil.getRole());
        assertEquals(1, perfil.getInstituicoes().size());
        assertEquals("Fatec São Paulo", perfil.getInstituicoes().get(0).getNome());
        assertEquals(1, perfil.getProjetos().size());
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException quando usuário não existir")
    void deveLancarExceptionQuandoUsuarioNaoExistir() {
        UUID idInexistente = UUID.randomUUID();
        when(userRepository.findById(idInexistente)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> usuarioService.obterPerfilPublico(idInexistente));
    }
}
