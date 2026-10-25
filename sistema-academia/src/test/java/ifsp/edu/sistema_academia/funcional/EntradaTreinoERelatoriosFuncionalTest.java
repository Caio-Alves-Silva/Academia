package ifsp.edu.sistema_academia.funcional;

import ifsp.edu.sistema_academia.model.Modalidade;
import ifsp.edu.sistema_academia.support.FuncionalTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Testes funcionais de Check-in, Fichas de Treino e Relatórios (RF12, RF13, RF14, RF15, RF16). */
class EntradaTreinoERelatoriosFuncionalTest extends FuncionalTestBase {

    @Test
    @DisplayName("RF12: check-in permitido para aluno regular; recusado se sem matrícula ou com mensalidade vencida > 5 dias")
    void checkinERecusaPorBloqueio() throws Exception {
        Aluno alunoSemMatricula = novoAluno();

        // 1. Aluno sem matrícula ativa é recusado (422)
        postar("/api/checkins", tokenRecepcao(), mapa("alunoId", alunoSemMatricula.id()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.mensagem").value(org.hamcrest.Matchers.containsString("matrícula")));

        // 2. Aluno com matrícula em dia tem entrada liberada (201 Created)
        Aluno alunoRegular = novoAluno();
        long planoId = novoPlano(1, "100.00", 3, Modalidade.MUSCULACAO);
        matricular(alunoRegular.id(), planoId);

        postar("/api/checkins", tokenRecepcao(), mapa("alunoId", alunoRegular.id()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.alunoId").value(alunoRegular.id()))
                .andExpect(jsonPath("$.dataHoraEntrada", notNullValue()))
                .andExpect(jsonPath("$.acessoLiberado").value(true));

        // 3. Vencimento da mensalidade foi em 11/03. Avança 6 dias (17/03 -> mais de 5 dias em atraso)
        clock.avancarDias(6);

        // Aluno bloqueado por inadimplência: check-in é recusado (mas a tentativa negada é persistida com motivo)
        postar("/api/checkins", tokenRecepcao(), mapa("alunoId", alunoRegular.id()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.mensagem").value(org.hamcrest.Matchers.containsString("bloqueado")));
        assertThat(checkinRepository.findByAlunoIdOrderByDataHoraEntradaDesc(alunoRegular.id()))
                .first()
                .satisfies(recusado -> {
                    assertThat(recusado.getAcessoLiberado()).isFalse();
                    assertThat(recusado.getMotivoBloqueio()).contains("mensalidade vencida");
                });
    }

    @Test
    @DisplayName("RF13: instrutor monta ficha de treino; anterior vai para o histórico; aluno vê atual e histórico")
    void fichaDeTreinoEHistorico() throws Exception {
        Aluno aluno = novoAluno();
        long planoId = novoPlano(1, "100.00", 3, Modalidade.MUSCULACAO);
        matricular(aluno.id(), planoId);

        Object itens1 = List.of(
                mapa("exercicio", "Supino Reto", "series", 4, "repeticoes", 10, "cargaKg", 60.0),
                mapa("exercicio", "Agachamento", "series", 4, "repeticoes", 12, "cargaKg", 80.0)
        );

        // Instrutor cria a 1ª ficha
        postar("/api/alunos/" + aluno.id() + "/fichas", tokenInstrutor(),
                mapa("observacoes", "Ficha inicial de adaptação", "itens", itens1))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.atual").value(true))
                .andExpect(jsonPath("$.itens", hasSize(2)));

        // Aluno consulta a própria ficha atual
        consultar("/api/alunos/" + aluno.id() + "/fichas/atual", aluno.token())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.atual").value(true))
                .andExpect(jsonPath("$.observacoes").value("Ficha inicial de adaptação"));

        // Instrutor monta uma 2ª ficha para o mesmo aluno
        Object itens2 = List.of(
                mapa("exercicio", "Supino Inclinado", "series", 3, "repeticoes", 12, "cargaKg", 50.0)
        );
        postar("/api/alunos/" + aluno.id() + "/fichas", tokenInstrutor(),
                mapa("observacoes", "Ficha nova: hipertrofia", "itens", itens2))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.atual").value(true));

        // A ficha atual agora é a 2ª
        consultar("/api/alunos/" + aluno.id() + "/fichas/atual", aluno.token())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.observacoes").value("Ficha nova: hipertrofia"));

        // O histórico agora contém a 1ª ficha (com atual=false)
        consultar("/api/alunos/" + aluno.id() + "/fichas/historico", aluno.token())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].observacoes").value("Ficha inicial de adaptação"))
                .andExpect(jsonPath("$[0].atual").value(false));

        // Outro aluno não acessa fichas de terceiros (403 Forbidden)
        Aluno outro = novoAluno();
        consultar("/api/alunos/" + aluno.id() + "/fichas/atual", outro.token())
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("RF14, RF15, RF16: relatórios de inadimplentes, ocupação de aulas e frequência de entradas")
    void relatoriosDoSistema() throws Exception {
        // RF14: Inadimplentes
        Aluno alunoDevedor = novoAluno();
        long planoId = novoPlano(3, "100.00", 3, Modalidade.MUSCULACAO);
        matricular(alunoDevedor.id(), planoId);

        // Avança 2 dias após vencimento da 1ª mensalidade (13/03)
        clock.avancarDias(2);

        // Recepção consulta relatório de inadimplentes
        consultar("/api/relatorios/inadimplentes", tokenRecepcao())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].alunoId").value(alunoDevedor.id()))
                .andExpect(jsonPath("$[0].valorDevido").value(102.00)); // 100 + 2% multa

        // RF16: Frequência no mês
        // Paga a mensalidade para liberar checkin
        pagar(idMensalidade(matriculaRepository.findByAlunoIdOrderByDataInicioDescIdDesc(alunoDevedor.id()).get(0).getId(), 1));
        postar("/api/checkins", tokenRecepcao(), mapa("alunoId", alunoDevedor.id()))
                .andExpect(status().isCreated());

        consultar("/api/relatorios/frequencia?mes=2026-03", tokenRecepcao())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[?(@.alunoId == " + alunoDevedor.id() + ")].entradas").value(org.hamcrest.Matchers.hasItem(1)));

        // RF15: Ocupação média por modalidade
        consultar("/api/relatorios/ocupacao-aulas", tokenRecepcao())
                .andExpect(status().isOk());
    }
}
