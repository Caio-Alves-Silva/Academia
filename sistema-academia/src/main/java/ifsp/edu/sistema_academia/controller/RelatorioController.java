package ifsp.edu.sistema_academia.controller;

import ifsp.edu.sistema_academia.dto.response.FrequenciaAlunoResponse;
import ifsp.edu.sistema_academia.dto.response.InadimplenteResponse;
import ifsp.edu.sistema_academia.dto.response.OcupacaoModalidadeResponse;
import ifsp.edu.sistema_academia.service.RelatorioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/api/relatorios")
@Tag(name = "Relatórios", description = "Inadimplência, ocupação das aulas e frequência (RF14, RF15, RF16)")
public class RelatorioController {

    private final RelatorioService relatorioService;

    public RelatorioController(RelatorioService relatorioService) {
        this.relatorioService = relatorioService;
    }

    @GetMapping("/inadimplentes")
    @PreAuthorize("hasRole('RECEPCAO')")
    @Operation(summary = "Alunos inadimplentes, com o valor devido",
            description = "Considera mensalidades pendentes vencidas. O valor devido inclui a multa de 2%.")
    public List<InadimplenteResponse> inadimplentes() {
        return relatorioService.inadimplentes();
    }

    @GetMapping("/ocupacao-aulas")
    @PreAuthorize("hasAnyRole('RECEPCAO','INSTRUTOR')")
    @Operation(summary = "Ocupação média das aulas coletivas por modalidade",
            description = "Média de inscritos confirmados / capacidade nas ocorrências do período. "
                    + "Padrão: últimos 30 dias.")
    public List<OcupacaoModalidadeResponse> ocupacao(
            @Parameter(description = "Início do período (yyyy-MM-dd)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @Parameter(description = "Fim do período (yyyy-MM-dd)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {
        return relatorioService.ocupacaoPorModalidade(inicio, fim);
    }

    @GetMapping("/frequencia")
    @PreAuthorize("hasRole('RECEPCAO')")
    @Operation(summary = "Frequência de entradas por aluno no mês",
            description = "Padrão: mês corrente. Inclui alunos sem nenhuma entrada.")
    public List<FrequenciaAlunoResponse> frequencia(
            @Parameter(description = "Mês no formato yyyy-MM", example = "2026-03")
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth mes) {
        return relatorioService.frequenciaPorAluno(mes);
    }
}