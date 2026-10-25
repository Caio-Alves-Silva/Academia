package ifsp.edu.sistema_academia.funcional;

import ifsp.edu.sistema_academia.model.Modalidade;
import ifsp.edu.sistema_academia.model.StatusMatricula;
import ifsp.edu.sistema_academia.model.StatusPagamento;
import ifsp.edu.sistema_academia.support.FuncionalTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Testes funcionais de Planos, Matrículas e Mensalidades (RF03, RF04, RF05, RF06, RF07). */
class PlanosEMatriculasFuncionalTest extends FuncionalTestBase {

    @Test
    @DisplayName("RF03: recepção cadastra planos com duração 1, 3 ou 12 meses; duração inválida gera 400")
    void cadastroDePlanos() throws Exception {
        // Sucesso: 1, 3 e 12 meses
        postar("/api/planos", tokenRecepcao(), mapa(
                "nome", "Plano Trimestral", "duracaoMeses", 3, "valorMensal", 99.90,
                "modalidades", List.of("MUSCULACAO", "SPINNING"), "limiteAulasSemana", 4))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.nome").value("Plano Trimestral"))
                .andExpect(jsonPath("$.duracaoMeses").value(3));

        // Falha: duração 6 meses não permitida (RF03)
        postar("/api/planos", tokenRecepcao(), mapa(
                "nome", "Plano Semestral Inválido", "duracaoMeses", 6, "valorMensal", 110.00,
                "modalidades", List.of("MUSCULACAO"), "limiteAulasSemana", 3))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[?(@.campo == 'duracaoMeses')]").exists());

        // Listar planos autenticado
        Aluno aluno = novoAluno();
        consultar("/api/planos", aluno.token())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    @DisplayName("RF04: matrícula gera mensalidades da vigência e bloqueia segunda matrícula ativa")
    void matricularAluno() throws Exception {
        Aluno aluno = novoAluno();
        long planoId = novoPlano(3, "90.00", 3, Modalidade.MUSCULACAO, Modalidade.SPINNING);

        ResultActions resposta = postar("/api/matriculas", tokenRecepcao(),
                mapa("alunoId", aluno.id(), "planoId", planoId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.alunoId").value(aluno.id()))
                .andExpect(jsonPath("$.status").value("ATIVA"))
                .andExpect(jsonPath("$.mensalidades", hasSize(3)));

        long matriculaId = lerId(resposta, "$.id");

        // Tentativa de segunda matrícula ativa para o mesmo aluno é rejeitada (409 Conflict)
        postar("/api/matriculas", tokenRecepcao(), mapa("alunoId", aluno.id(), "planoId", planoId))
                .andExpect(status().isConflict());

        // Aluno consulta a própria matrícula
        consultar("/api/matriculas/" + matriculaId, aluno.token())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alunoId").value(aluno.id()));

        // Outro aluno não tem acesso à matrícula alheia (403 Forbidden)
        Aluno outro = novoAluno();
        consultar("/api/matriculas/" + matriculaId, outro.token())
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("RF05: pagar mensalidade em dia e com atraso (multa de 2%)")
    void pagamentoDeMensalidade() throws Exception {
        Aluno aluno = novoAluno();
        long planoId = novoPlano(3, "100.00", 3, Modalidade.MUSCULACAO);
        long matriculaId = matricular(aluno.id(), planoId);

        long m1Id = idMensalidade(matriculaId, 1);
        long m2Id = idMensalidade(matriculaId, 2);

        // Pagamento da 1ª mensalidade no dia do vencimento (sem multa)
        postar("/api/mensalidades/" + m1Id + "/pagamento", aluno.token(), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAGO"))
                .andExpect(jsonPath("$.multa").value(0))
                .andExpect(jsonPath("$.valorPago").value(100.00));

        // Avança o tempo para depois do vencimento da 2ª parcela (vencimento em 11/04, avança para 13/04)
        clock.avancarDias(33);

        // Pagamento com atraso: multa de 2% (2.00 em 100.00 -> total 102.00)
        postar("/api/mensalidades/" + m2Id + "/pagamento", aluno.token(), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAGO"))
                .andExpect(jsonPath("$.multa").value(2.00))
                .andExpect(jsonPath("$.valorPago").value(102.00));
    }

    @Test
    @DisplayName("RF06: trocar de plano encerra matrícula anterior e concede crédito proporcional na nova")
    void trocarDePlanoComDescontoProporcional() throws Exception {
        Aluno aluno = novoAluno();
        long planoAtual = novoPlano(3, "90.00", 3, Modalidade.MUSCULACAO);
        long planoNovo = novoPlano(12, "120.00", 5, Modalidade.MUSCULACAO, Modalidade.SPINNING);
        long matriculaId = matricular(aluno.id(), planoAtual);

        long m1Id = idMensalidade(matriculaId, 1);
        pagar(m1Id);

        // Avança 10 dias após início
        clock.avancarDias(10);

        // Troca de plano
        ResultActions resposta = postar("/api/matriculas/" + matriculaId + "/troca-plano", tokenRecepcao(),
                mapa("novoPlanoId", planoNovo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matriculaEncerrada.status").value("ENCERRADA"))
                .andExpect(jsonPath("$.novaMatricula.status").value("ATIVA"))
                .andExpect(jsonPath("$.mensalidadesCanceladas").value(2));

        Number credito = ler(resposta, "$.creditoCalculado");
        Number desconto = ler(resposta, "$.descontoAplicado");
        assertThat(credito.doubleValue()).isGreaterThan(0.0);
        assertThat(desconto.doubleValue()).isGreaterThan(0.0);
    }

    @Test
    @DisplayName("RF07: cancelamento de matrícula cancela mensalidades futuras e aplica multa de 10% para 3 ou 12 meses")
    void cancelarMatriculaComMulta() throws Exception {
        Aluno aluno = novoAluno();
        long planoTrimestral = novoPlano(3, "100.00", 3, Modalidade.MUSCULACAO);
        long matriculaId = matricular(aluno.id(), planoTrimestral);

        pagar(idMensalidade(matriculaId, 1));

        // Cancela no 1º mês: restam 2 mensalidades de 100.00 futuras (total 200.00). Multa de 10% = 20.00
        postar("/api/matriculas/" + matriculaId + "/cancelamento", tokenRecepcao(), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matricula.status").value("CANCELADA"))
                .andExpect(jsonPath("$.mensalidadesCanceladas").value(2))
                .andExpect(jsonPath("$.valorMensalidadesCanceladas").value(200.00))
                .andExpect(jsonPath("$.multaCancelamento").value(20.00));
    }
}
