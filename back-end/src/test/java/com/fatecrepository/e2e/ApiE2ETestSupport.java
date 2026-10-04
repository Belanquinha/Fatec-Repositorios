package com.fatecrepository.e2e;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fatecrepository.model.Instituicao;
import com.fatecrepository.model.User;
import com.fatecrepository.model.UserRole;
import com.fatecrepository.repository.InstituicaoRepository;
import com.fatecrepository.repository.ProjetoRepository;
import com.fatecrepository.repository.UserRepository;
import com.fatecrepository.security.JwtTokenProvider;
import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Base das suítes E2E de API.
 *
 * <p>Sobe a aplicação Spring completa (web + segurança + JPA) em porta aleatória
 * contra o PostgreSQL de teste ({@code application-test.yml}) e fala HTTP de verdade
 * via {@link RestTemplate}. Assim o filtro JWT, os interceptors de autorização, os
 * controllers, os services e o banco são exercitados de ponta a ponta — que é o que
 * a validação dos Critérios de Aceite exige.
 *
 * <p>Nenhum teste aqui depende da rede Microsoft: os tokens de sessão são emitidos
 * pelo próprio {@link JwtTokenProvider} do projeto, usando o segredo do perfil
 * {@code test}. O que está sendo validado é a autorização do <em>nosso</em> backend,
 * não o provedor de identidade externo (esse é coberto por
 * {@code MicrosoftTokenVerifierTest}).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
abstract class ApiE2ETestSupport {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @LocalServerPort
    protected int port;

    @Autowired
    protected JwtTokenProvider jwtTokenProvider;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected InstituicaoRepository instituicaoRepository;

    @Autowired
    protected ProjetoRepository projetoRepository;

    /**
     * O Boot 4 não auto-configura {@code RestTemplate} nesta combinação de jars,
     * então instanciamos direto.
     *
     * <p>Dois detalhes importam aqui:
     * <ul>
     *   <li>a request factory é a do JDK, porque a do {@code SimpleClientHttpRequestFactory}
     *       ({@code HttpURLConnection}) descarta o corpo das respostas 4xx — e o corpo
     *       da resposta 401/403 é justamente o que我们要 asseverar;</li>
     *   <li>o error handler é desligado porque 401/403/404/400 são resultados
     *       esperados e precisam ser asseridos, não lançados como exceção.</li>
     * </ul>
     */
    protected final RestTemplate restTemplate = criarRestTemplate();

    private static RestTemplate criarRestTemplate() {
        RestTemplate template = new RestTemplate(new JdkClientHttpRequestFactory());
        template.setErrorHandler(new DefaultResponseErrorHandler() {
            @Override
            public boolean hasError(ClientHttpResponse response) {
                return false;
            }
        });
        return template;
    }

    /**
     * Isola cada teste: o perfil usa {@code ddl-auto=create-drop}, mas limpamos as
     * tabelas a cada caso para que um teste nunca dependa do resíduo do anterior.
     */
    @BeforeEach
    void limparBase() {
        projetoRepository.deleteAll();
        instituicaoRepository.deleteAll();
        userRepository.deleteAll();
    }

    // ------------------------------------------------------------------
    // HTTP helpers
    // ------------------------------------------------------------------

    protected String url(String path) {
        return "http://localhost:" + port + path;
    }

    protected ResponseEntity<String> get(String path) {
        return get(path, null);
    }

    protected ResponseEntity<String> get(String path, String token) {
        return exchange(HttpMethod.GET, path, null, token);
    }

    protected ResponseEntity<String> post(String path, Object body, String token) {
        return exchange(HttpMethod.POST, path, body, token);
    }

    protected ResponseEntity<String> put(String path, Object body, String token) {
        return exchange(HttpMethod.PUT, path, body, token);
    }

    protected ResponseEntity<String> delete(String path, String token) {
        return exchange(HttpMethod.DELETE, path, null, token);
    }

    private ResponseEntity<String> exchange(HttpMethod method, String path, Object body, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        if (token != null) {
            headers.setBearerAuth(token);
        }
        return restTemplate.exchange(url(path), method, new HttpEntity<>(body, headers), String.class);
    }

    // ------------------------------------------------------------------
    // Sessão / usuários
    // ------------------------------------------------------------------

    protected User criarUsuario(String nome, String email, UserRole role) {
        User user = new User();
        user.setNome(nome);
        user.setEmail(email);
        user.setRole(role);
        return userRepository.saveAndFlush(user);
    }

    /** Emite um token de sessão real para o usuário persistido. */
    protected String tokenDe(User user) {
        return jwtTokenProvider.generateToken(user);
    }

    protected String tokenPara(String nome, String email, UserRole role) {
        return tokenDe(criarUsuario(nome, email, role));
    }

    // ------------------------------------------------------------------
    // Instalações de teste
    // ------------------------------------------------------------------

    protected Instituicao criarInstituicao(String codigoUnidade, String nome) {
        return criarInstituicao(codigoUnidade, nome, true);
    }

    protected Instituicao criarInstituicao(String codigoUnidade, String nome, boolean ativo) {
        Instituicao instituicao = new Instituicao();
        instituicao.setCodigoUnidade(codigoUnidade);
        instituicao.setNome(nome);
        instituicao.setAtivo(ativo);
        instituicao.setEstado("SP");
        return instituicaoRepository.saveAndFlush(instituicao);
    }

    // ------------------------------------------------------------------
    // JSON helpers
    // ------------------------------------------------------------------

    protected static JsonNode json(ResponseEntity<String> response) {
        try {
            return MAPPER.readTree(response.getBody());
        } catch (Exception e) {
            throw new AssertionError("Resposta não é JSON válido: " + response.getBody(), e);
        }
    }

    protected static List<Map<String, Object>> jsonList(ResponseEntity<String> response) {
        try {
            return MAPPER.readValue(response.getBody(), new TypeReference<>() {
            });
        } catch (Exception e) {
            throw new AssertionError("Resposta não é um array JSON válido: " + response.getBody(), e);
        }
    }

    protected static String texto(ResponseEntity<String> response, String campo) {
        return json(response).path(campo).asText();
    }

    protected static void assertNaoAutorizado(ResponseEntity<String> response) {
        assertThat(response.getStatusCode().value())
            .as("Esperado 401 (Não autorizado). Corpo: %s", response.getBody())
            .isEqualTo(401);
    }

    protected static void assertAcessoNegado(ResponseEntity<String> response) {
        assertThat(response.getStatusCode().value())
            .as("Esperado 403 (Acesso negado). Corpo: %s", response.getBody())
            .isEqualTo(403);
    }
}