package ifsp.edu.sistema_academia.support;

import ifsp.edu.sistema_academia.model.Instrutor;

import ifsp.edu.sistema_academia.model.Aluno;

import ifsp.edu.sistema_academia.model.AulaColetiva;
import ifsp.edu.sistema_academia.model.Matricula;
import ifsp.edu.sistema_academia.model.Mensalidade;
import ifsp.edu.sistema_academia.model.Modalidade;
import ifsp.edu.sistema_academia.model.Perfil;
import ifsp.edu.sistema_academia.model.Plano;
import ifsp.edu.sistema_academia.model.Usuario;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

/** Fábricas de objetos de domínio para os testes unitários (os IDs são definidos por reflexão). */
public final class Fixtures {

    public static final ZoneId ZONA = ZoneId.of("America/Sao_Paulo");
    /** Quarta-feira, 11/03/2026, 10h: a semana corrente vai de 09/03 (segunda) a 15/03 (domingo). */
    public static final LocalDateTime AGORA = LocalDateTime.of(2026, 3, 11, 10, 0);

    private Fixtures() {
    }

    public static Clock relogio(LocalDateTime dataHora) {
        return Clock.fixed(dataHora.atZone(ZONA).toInstant(), ZONA);
    }

    public static <T> T comId(T entidade, Long id) {
        ReflectionTestUtils.setField(entidade, "id", id);
        return entidade;
    }

    public static Aluno aluno(Long id) {
        return comId(new Aluno("Aluno " + id, "0000000000" + id, "aluno" + id + "@teste.com", "hash", AGORA), id);
    }

    public static Instrutor instrutor(Long id) {
        return comId(new Instrutor("Instrutor " + id, "1000000000" + id, "instrutor" + id + "@teste.com", "hash",
                AGORA), id);
    }

    public static Plano plano(Long id, int meses, String valorMensal, int limiteSemanal, Modalidade... modalidades) {
        return comId(new Plano("Plano " + id, meses, new BigDecimal(valorMensal),
                new HashSet<>(Arrays.asList(modalidades)), limiteSemanal), id);
    }

    public static AulaColetiva aula(Long id, Instrutor instrutor, Modalidade modalidade, DayOfWeek dia,
                                    LocalTime horario, int duracao, int capacidade, LocalDate criadaEm) {
        return comId(new AulaColetiva(modalidade, instrutor, dia, horario, duracao, capacidade, criadaEm), id);
    }

    /** Matrícula iniciada em {@code inicio}, com IDs atribuídos à matrícula e às mensalidades. */
    public static Matricula matricula(Long id, Aluno aluno, Plano plano, LocalDate inicio) {
        Matricula matricula = Matricula.iniciar(aluno, plano, inicio, BigDecimal.ZERO);
        comId(matricula, id);
        List<Mensalidade> mensalidades = matricula.getMensalidades();
        for (int i = 0; i < mensalidades.size(); i++) {
            comId(mensalidades.get(i), id * 100 + i + 1);
        }
        return matricula;
    }
}