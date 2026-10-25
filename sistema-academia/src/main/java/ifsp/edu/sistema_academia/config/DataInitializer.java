package ifsp.edu.sistema_academia.config;

import ifsp.edu.sistema_academia.model.Aluno;
import ifsp.edu.sistema_academia.model.AulaColetiva;
import ifsp.edu.sistema_academia.model.Instrutor;
import ifsp.edu.sistema_academia.model.Modalidade;
import ifsp.edu.sistema_academia.model.Plano;
import ifsp.edu.sistema_academia.model.Recepcao;
import ifsp.edu.sistema_academia.model.Turno;
import ifsp.edu.sistema_academia.model.Usuario;
import ifsp.edu.sistema_academia.repository.AulaColetivaRepository;
import ifsp.edu.sistema_academia.repository.PlanoRepository;
import ifsp.edu.sistema_academia.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.Set;

/**
 * Carga inicial complementar ao Flyway ({@code V2__carga_inicial.sql}); é idempotente. Sempre garante as contas de
 * recepção e instrutores (que não se cadastram pela API) - útil quando o schema é criado pelo Hibernate (perfil
 * {@code h2} e testes). Com {@code app.seed.demo-data=true} cria também planos (se não houver nenhum), aulas e um
 * aluno de demonstração.
 */
@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UsuarioRepository usuarioRepository;
    private final PlanoRepository planoRepository;
    private final AulaColetivaRepository aulaRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;
    private final boolean demoData;
    private final String senhaRecepcao;
    private final String senhaInstrutor;
    private final String senhaAluno;

    public DataInitializer(UsuarioRepository usuarioRepository, PlanoRepository planoRepository,
                           AulaColetivaRepository aulaRepository, PasswordEncoder passwordEncoder, Clock clock,
                           @Value("${app.seed.demo-data:true}") boolean demoData,
                           @Value("${app.seed.senha-recepcao:Recepcao@123}") String senhaRecepcao,
                           @Value("${app.seed.senha-instrutor:Instrutor@123}") String senhaInstrutor,
                           @Value("${app.seed.senha-aluno:Aluno@1234}") String senhaAluno) {
        this.usuarioRepository = usuarioRepository;
        this.planoRepository = planoRepository;
        this.aulaRepository = aulaRepository;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
        this.demoData = demoData;
        this.senhaRecepcao = senhaRecepcao;
        this.senhaInstrutor = senhaInstrutor;
        this.senhaAluno = senhaAluno;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        LocalDateTime agora = LocalDateTime.now(clock);
        garantirUsuario("recepcao@academia.com", () -> {
            Recepcao recepcao = new Recepcao("Recepção Academia", "11144477735", "recepcao@academia.com",
                    passwordEncoder.encode(senhaRecepcao), agora);
            recepcao.setIdFunc("REC-001");
            recepcao.setTurno(Turno.MANHA);
            recepcao.setRamal("100");
            return recepcao;
        });
        Instrutor instrutor1 = (Instrutor) garantirUsuario("instrutor@academia.com", () ->
                novoInstrutor("Carlos Instrutor", "52998224725", "instrutor@academia.com", "000001-G/SP",
                        "Musculação", Turno.MANHA, agora));
        Instrutor instrutor2 = (Instrutor) garantirUsuario("instrutor2@academia.com", () ->
                novoInstrutor("Ana Instrutora", "39053344705", "instrutor2@academia.com", "000002-G/SP",
                        "Yoga e Pilates", Turno.NOITE, agora));
        if (demoData) {
            criarDadosDeDemonstracao(instrutor1, instrutor2, agora);
        }
    }

    private Instrutor novoInstrutor(String nome, String cpf, String email, String cref, String especialidade,
                                    Turno turno, LocalDateTime agora) {
        Instrutor instrutor = new Instrutor(nome, cpf, email, passwordEncoder.encode(senhaInstrutor), agora);
        instrutor.setCref(cref);
        instrutor.setEspecialidade(especialidade);
        instrutor.setTurno(turno);
        instrutor.setDataContratacao(agora.toLocalDate());
        instrutor.setAtivo(true);
        return instrutor;
    }

    private Usuario garantirUsuario(String email, java.util.function.Supplier<Usuario> fabrica) {
        return usuarioRepository.findByEmail(email).orElseGet(() -> {
            Usuario novo = usuarioRepository.save(fabrica.get());
            log.info("Carga inicial: criando usuário {} ({})", email, novo.getPerfil());
            return novo;
        });
    }

    private void criarDadosDeDemonstracao(Instrutor instrutor1, Instrutor instrutor2, LocalDateTime agora) {
        if (planoRepository.count() == 0) {
            planoRepository.save(new Plano("Mensal Básico", 1, new BigDecimal("99.90"),
                    Set.of(Modalidade.MUSCULACAO, Modalidade.SPINNING), 3));
            planoRepository.save(new Plano("Trimestral Completo", 3, new BigDecimal("89.90"),
                    Set.of(Modalidade.MUSCULACAO, Modalidade.SPINNING, Modalidade.FUNCIONAL, Modalidade.YOGA), 4));
            planoRepository.save(new Plano("Anual Premium", 12, new BigDecimal("69.90"),
                    EnumSet.allOf(Modalidade.class), 7));
        }
        if (aulaRepository.count() == 0) {
            garantirUsuario("aluno@academia.com", () -> {
                Aluno aluno = new Aluno("Aluno Demonstração", "16899535009", "aluno@academia.com",
                        passwordEncoder.encode(senhaAluno), agora);
                aluno.setObjetivo("Condicionamento físico");
                return aluno;
            });
            LocalDate hoje = agora.toLocalDate();
            aulaRepository.save(new AulaColetiva(Modalidade.SPINNING, instrutor1, DayOfWeek.MONDAY,
                    LocalTime.of(18, 0), 50, 15, hoje));
            aulaRepository.save(new AulaColetiva(Modalidade.YOGA, instrutor2, DayOfWeek.WEDNESDAY,
                    LocalTime.of(19, 0), 60, 20, hoje));
            aulaRepository.save(new AulaColetiva(Modalidade.FUNCIONAL, instrutor1, DayOfWeek.FRIDAY,
                    LocalTime.of(7, 0), 45, 12, hoje));
            log.info("Carga inicial: planos, aulas e aluno de demonstração criados");
        }
    }
}
