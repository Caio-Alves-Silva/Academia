package ifsp.edu.sistema_academia.funcional;

import ifsp.edu.sistema_academia.model.Modalidade;
import ifsp.edu.sistema_academia.model.StatusPresenca;
import ifsp.edu.sistema_academia.model.StatusInscricao;
import ifsp.edu.sistema_academia.support.FuncionalTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Testes funcionais de Aulas Coletivas, Inscrições e Presenças (RF08, RF09, RF10, RF11). */
class AulasEInscricoesFuncionalTest extends FuncionalTestBase {

    private static final LocalDate SEXTA = LocalDate.of(2026, 3, 13);
    private static final LocalDate QUARTA = LocalDate.of(2026, 3, 11);
    private static final LocalDate QUINTA = LocalDate.of(2026, 3, 12);

    @Test
    @DisplayName("RF08: recepção cadastra aula coletiva e bloqueia horários sobrepostos para o mesmo instrutor")
    void cadastroDeAulaEConflitoDeHorario() throws Exception {
        Long instrutor1 = idInstrutor();
        Long instrutor2 = idInstrutor2();

        // Cadastra aula de Spinning segunda-feira 18:00 com 50 min (término 18:50)
        novaAula(Modalidade.SPINNING, instrutor1, DayOfWeek.MONDAY, LocalTime.of(18, 0), 50, 15);

        // Conflito: mesma segunda-feira, 18:30 (dentro de 18:00 - 18:50), mesmo instrutor -> 409 Conflict
        postar("/api/aulas", tokenRecepcao(), mapa(
                "modalidade", "SPINNING", "instrutorId", instrutor1, "diaSemana", DayOfWeek.MONDAY,
                "horario", "18:30", "duracaoMinutos", 45, "capacidade", 10))
                .andExpect(status().isConflict());

        // Permitido: mesmo horário mas com outro instrutor (instrutor 2)
        postar("/api/aulas", tokenRecepcao(), mapa(
                "modalidade", "YOGA", "instrutorId", instrutor2, "diaSemana", DayOfWeek.MONDAY,
                "horario", "18:00", "duracaoMinutos", 50, "capacidade", 12))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("RF09: inscrição em aula na semana corrente e lista de espera quando lotada")
    void inscricaoEListaDeEspera() throws Exception {
        long planoId = novoPlano(1, "100.00", 3, Modalidade.SPINNING, Modalidade.MUSCULACAO);
        Aluno aluno1 = novoAluno();
        Aluno aluno2 = novoAluno();
        matricular(aluno1.id(), planoId);
        matricular(aluno2.id(), planoId);

        // Aula na sexta-feira 13/03 com capacidade para 1 aluno
        long aulaId = novaAula(Modalidade.SPINNING, idInstrutor(), DayOfWeek.FRIDAY, LocalTime.of(18, 0), 50, 1);

        // Aluno 1 se inscreve -> vaga garantida (CONFIRMADA)
        postar("/api/aulas/" + aulaId + "/inscricoes", aluno1.token(), mapa("data", SEXTA.toString()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CONFIRMADA"))
                .andExpect(jsonPath("$.posicaoListaEspera").doesNotExist());

        // Aluno 2 se inscreve -> sem vaga, entra na lista de espera (LISTA_ESPERA, posicao 1)
        postar("/api/aulas/" + aulaId + "/inscricoes", aluno2.token(), mapa("data", SEXTA.toString()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("LISTA_ESPERA"))
                .andExpect(jsonPath("$.posicaoListaEspera").value(1));

        // Inscrição duplicada na mesma aula/data -> 409 Conflict
        postar("/api/aulas/" + aulaId + "/inscricoes", aluno1.token(), mapa("data", SEXTA.toString()))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("RF10: cancelar inscrição promove automaticamente o primeiro da lista de espera")
    void cancelamentoComPromocaoAutomatica() throws Exception {
        long planoId = novoPlano(1, "100.00", 3, Modalidade.SPINNING, Modalidade.MUSCULACAO);
        Aluno aluno1 = novoAluno();
        Aluno aluno2 = novoAluno();
        matricular(aluno1.id(), planoId);
        matricular(aluno2.id(), planoId);

        // Aula na sexta-feira 13/03 às 18:00 com capacidade 1
        long aulaId = novaAula(Modalidade.SPINNING, idInstrutor(), DayOfWeek.FRIDAY, LocalTime.of(18, 0), 50, 1);

        ResultActions r1 = postar("/api/aulas/" + aulaId + "/inscricoes", aluno1.token(), mapa("data", SEXTA.toString()))
                .andExpect(status().isCreated());
        long inscricao1Id = lerId(r1, "$.id");

        ResultActions r2 = postar("/api/aulas/" + aulaId + "/inscricoes", aluno2.token(), mapa("data", SEXTA.toString()))
                .andExpect(status().isCreated());
        long inscricao2Id = lerId(r2, "$.id");

        // Aluno 1 cancela (estamos em quarta-feira 10h, mais de 2 horas antes de sexta 18h)
        postar("/api/inscricoes/" + inscricao1Id + "/cancelamento", aluno1.token(), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inscricaoCancelada.status").value("CANCELADA"))
                .andExpect(jsonPath("$.inscricaoPromovida.alunoId").value(aluno2.id()))
                .andExpect(jsonPath("$.inscricaoPromovida.status").value("CONFIRMADA"));

        // Verifica que o aluno 2 agora está com status CONFIRMADA
        consultar("/api/alunos/" + aluno2.id() + "/inscricoes", aluno2.token())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("CONFIRMADA"));
    }

    @Test
    @DisplayName("RF11: instrutor registra presença e falta; 2 faltas na mesma semana bloqueiam inscrição na semana seguinte")
    void presencaEBloqueioPorDuasFaltas() throws Exception {
        clock.definir(java.time.LocalDateTime.of(2026, 3, 11, 7, 0));
        long planoId = novoPlano(1, "100.00", 5, Modalidade.SPINNING, Modalidade.MUSCULACAO);
        Aluno aluno1 = novoAluno();
        matricular(aluno1.id(), planoId);

        // 1ª aula: Quarta 08:00 (ainda vai começar)
        long aula1 = novaAula(Modalidade.SPINNING, idInstrutor(), DayOfWeek.WEDNESDAY, LocalTime.of(8, 0), 50, 10);
        postar("/api/aulas/" + aula1 + "/inscricoes", aluno1.token(), mapa("data", QUARTA.toString()))
                .andExpect(status().isCreated());

        // Relógio avança para Quarta 09:00 (aula já terminou)
        clock.avancarHoras(2);

        // Instrutor registra presença sem o aluno1 -> Aluno1 leva FALTA 1
        postar("/api/aulas/" + aula1 + "/presencas", tokenInstrutor(),
                mapa("data", QUARTA.toString(), "alunosPresentes", List.of()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.faltas").value(1));

        // 2ª aula: Quinta 08:00
        long aula2 = novaAula(Modalidade.SPINNING, idInstrutor(), DayOfWeek.THURSDAY, LocalTime.of(8, 0), 50, 10);
        postar("/api/aulas/" + aula2 + "/inscricoes", aluno1.token(), mapa("data", QUINTA.toString()))
                .andExpect(status().isCreated());

        // Avança o relógio para quinta-feira 09:00
        clock.avancarDias(1);
        postar("/api/aulas/" + aula2 + "/presencas", tokenInstrutor(),
                mapa("data", QUINTA.toString(), "alunosPresentes", List.of()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.faltas").value(1));

        // Agora o aluno1 tem 2 faltas na semana (09/03 a 15/03).
        // Avança o relógio para a semana seguinte (segunda-feira 16/03)
        clock.avancarDias(4);

        // Cadastra aula na semana seguinte (terça-feira 17/03)
        long aulaSeguinte = novaAula(Modalidade.SPINNING, idInstrutor(), DayOfWeek.TUESDAY, LocalTime.of(10, 0), 50, 10);

        // Tentativa de inscrição na semana seguinte é bloqueada por 2 faltas (422 BloqueioPorFaltasException)
        LocalDate tercaSeguinte = LocalDate.of(2026, 3, 17);
        postar("/api/aulas/" + aulaSeguinte + "/inscricoes", aluno1.token(), mapa("data", tercaSeguinte.toString()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.mensagem").value(org.hamcrest.Matchers.containsString("bloqueada")));
    }
}
