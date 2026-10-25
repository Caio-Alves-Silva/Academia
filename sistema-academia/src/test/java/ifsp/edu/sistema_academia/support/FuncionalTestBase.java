package ifsp.edu.sistema_academia.support;

import com.jayway.jsonpath.JsonPath;
import ifsp.edu.sistema_academia.model.Modalidade;
import ifsp.edu.sistema_academia.model.Perfil;
import ifsp.edu.sistema_academia.repository.AulaColetivaRepository;
import ifsp.edu.sistema_academia.repository.CheckinRepository;
import ifsp.edu.sistema_academia.repository.FichaTreinoRepository;
import ifsp.edu.sistema_academia.repository.InscricaoRepository;
import ifsp.edu.sistema_academia.repository.MatriculaRepository;
import ifsp.edu.sistema_academia.repository.MensalidadeRepository;
import ifsp.edu.sistema_academia.repository.PlanoRepository;
import ifsp.edu.sistema_academia.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base dos testes funcionais: sobe a aplicação completa (Spring Security + JWT + JPA/H2) e chama a API por HTTP
 * simulado (MockMvc). Cada teste começa com o banco limpo (exceto as contas de recepção e instrutores da carga
 * inicial) e com o relógio em quarta-feira, 11/03/2026, 10h (semana corrente: 09/03 a 15/03).
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestClockConfig.class)
public abstract class FuncionalTestBase {

    protected static final String SENHA = "senha1234";
    protected static final LocalDateTime INICIO = Fixtures.AGORA;
    private static final AtomicLong SEQUENCIA = new AtomicLong(1);

    @Autowired
    protected WebApplicationContext contexto;
    @Autowired
    protected ObjectMapper json;
    @Autowired
    protected MutableClock clock;
    @Autowired
    protected UsuarioRepository usuarioRepository;
    @Autowired
    protected PlanoRepository planoRepository;
    @Autowired
    protected MatriculaRepository matriculaRepository;
    @Autowired
    protected MensalidadeRepository mensalidadeRepository;
    @Autowired
    protected AulaColetivaRepository aulaRepository;
    @Autowired
    protected InscricaoRepository inscricaoRepository;
    @Autowired
    protected CheckinRepository checkinRepository;
    @Autowired
    protected FichaTreinoRepository fichaRepository;

    protected MockMvc mvc;

    public record Aluno(Long id, String email, String token) {
    }

    @BeforeEach
    void prepararAmbiente() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
        clock.definir(INICIO);
        inscricaoRepository.deleteAll();
        checkinRepository.deleteAll();
        fichaRepository.deleteAll();
        matriculaRepository.deleteAll();
        aulaRepository.deleteAll();
        planoRepository.deleteAll();
        usuarioRepository.deleteAll(usuarioRepository.findByPerfilOrderByNomeAsc(Perfil.ALUNO));
    }

    // ---------- requisições ----------

    protected ResultActions chamar(MockHttpServletRequestBuilder requisicao, String token, Object corpo)
            throws Exception {
        if (token != null) {
            requisicao.header("Authorization", "Bearer " + token);
        }
        if (corpo != null) {
            requisicao.contentType(MediaType.APPLICATION_JSON)
                    .content(corpo instanceof String texto ? texto : json.writeValueAsString(corpo));
        }
        return mvc.perform(requisicao);
    }

    protected ResultActions postar(String url, String token, Object corpo) throws Exception {
        return chamar(post(url), token, corpo);
    }

    protected ResultActions consultar(String url, String token) throws Exception {
        return chamar(get(url), token, null);
    }

    protected static String corpo(ResultActions resultado) throws Exception {
        return resultado.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    protected static <T> T ler(ResultActions resultado, String caminho) throws Exception {
        return JsonPath.read(corpo(resultado), caminho);
    }

    protected static long lerId(ResultActions resultado, String caminho) throws Exception {
        Number numero = ler(resultado, caminho);
        return numero.longValue();
    }

    protected static Map<String, Object> mapa(Object... paresChaveValor) {
        Map<String, Object> mapa = new LinkedHashMap<>();
        for (int i = 0; i < paresChaveValor.length; i += 2) {
            mapa.put((String) paresChaveValor[i], paresChaveValor[i + 1]);
        }
        return mapa;
    }

    // ---------- autenticação ----------

    protected String login(String email, String senha) throws Exception {
        ResultActions resultado = postar("/api/auth/login", null, mapa("email", email, "senha", senha))
                .andExpect(status().isOk());
        return ler(resultado, "$.token");
    }

    protected String tokenRecepcao() throws Exception {
        return login("recepcao@academia.com", "Recepcao@123");
    }

    protected String tokenInstrutor() throws Exception {
        return login("instrutor@academia.com", "Instrutor@123");
    }

    protected String tokenInstrutor2() throws Exception {
        return login("instrutor2@academia.com", "Instrutor@123");
    }

    protected Long idInstrutor() {
        return usuarioRepository.findByEmail("instrutor@academia.com").orElseThrow().getId();
    }

    protected Long idInstrutor2() {
        return usuarioRepository.findByEmail("instrutor2@academia.com").orElseThrow().getId();
    }

    /** CPF válido, único por chamada. */
    protected static String cpfValido() {
        String base = String.format("%09d", 100_000_000L + SEQUENCIA.getAndIncrement() * 7);
        int d1 = digito(base, 10);
        int d2 = digito(base + d1, 11);
        return base + d1 + d2;
    }

    private static int digito(String numeros, int pesoInicial) {
        int soma = 0;
        for (int i = 0; i < numeros.length(); i++) {
            soma += (numeros.charAt(i) - '0') * (pesoInicial - i);
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }

    /** Cadastra um aluno pela API e faz login. */
    protected Aluno novoAluno() throws Exception {
        String email = "aluno" + SEQUENCIA.getAndIncrement() + "@teste.com";
        ResultActions cadastro = postar("/api/auth/cadastro", null,
                mapa("nome", "Aluno Teste", "cpf", cpfValido(), "email", email, "senha", SENHA))
                .andExpect(status().isCreated());
        return new Aluno(lerId(cadastro, "$.id"), email, login(email, SENHA));
    }

    // ---------- massa de dados via API ----------

    protected long novoPlano(int meses, String valorMensal, int limiteSemanal, Modalidade... modalidades)
            throws Exception {
        ResultActions resposta = postar("/api/planos", tokenRecepcao(), mapa(
                "nome", "Plano " + meses + "m", "duracaoMeses", meses, "valorMensal", new BigDecimal(valorMensal),
                "modalidades", List.of(modalidades), "limiteAulasSemana", limiteSemanal))
                .andExpect(status().isCreated());
        return lerId(resposta, "$.id");
    }

    protected long matricular(Long alunoId, long planoId) throws Exception {
        ResultActions resposta = postar("/api/matriculas", tokenRecepcao(), mapa("alunoId", alunoId, "planoId", planoId))
                .andExpect(status().isCreated());
        return lerId(resposta, "$.id");
    }

    protected long novaAula(Modalidade modalidade, Long instrutorId, DayOfWeek dia, LocalTime horario,
                            int duracao, int capacidade) throws Exception {
        ResultActions resposta = postar("/api/aulas", tokenRecepcao(), mapa(
                "modalidade", modalidade, "instrutorId", instrutorId, "diaSemana", dia, "horario",
                horario.toString(), "duracaoMinutos", duracao, "capacidade", capacidade))
                .andExpect(status().isCreated());
        return lerId(resposta, "$.id");
    }

    /** ID da mensalidade {@code numero} (1..n) da matrícula. */
    protected long idMensalidade(long matriculaId, int numero) throws Exception {
        ResultActions resposta = consultar("/api/matriculas/" + matriculaId, tokenRecepcao()).andExpect(status().isOk());
        return lerId(resposta, "$.mensalidades[" + (numero - 1) + "].id");
    }

    protected void pagar(long mensalidadeId) throws Exception {
        postar("/api/mensalidades/" + mensalidadeId + "/pagamento", tokenRecepcao(), null)
                .andExpect(status().isOk());
    }
}