package ifsp.edu.sistema_academia.controller;

import ifsp.edu.sistema_academia.dto.request.MatriculaRequest;
import ifsp.edu.sistema_academia.dto.request.TrocaPlanoRequest;
import ifsp.edu.sistema_academia.dto.response.CancelamentoMatriculaResponse;
import ifsp.edu.sistema_academia.dto.response.MatriculaResponse;
import ifsp.edu.sistema_academia.dto.response.TrocaPlanoResponse;
import ifsp.edu.sistema_academia.security.UsuarioAutenticado;
import ifsp.edu.sistema_academia.security.UsuarioLogado;
import ifsp.edu.sistema_academia.service.MatriculaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Matrículas", description = "Contrato do aluno com um plano (RF04, RF06, RF07)")
public class MatriculaController {

    private final MatriculaService matriculaService;

    public MatriculaController(MatriculaService matriculaService) {
        this.matriculaService = matriculaService;
    }

    @PostMapping("/matriculas")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('RECEPCAO')")
    @Operation(summary = "Matricula um aluno em um plano",
            description = "Gera todas as mensalidades da vigência (vencimento no mesmo dia de cada mês). "
                    + "O aluno só pode ter uma matrícula ativa.")
    public MatriculaResponse matricular(@Valid @RequestBody MatriculaRequest request) {
        return matriculaService.matricular(request);
    }

    @GetMapping("/matriculas/{id}")
    @PreAuthorize("hasAnyRole('ALUNO','RECEPCAO')")
    @Operation(summary = "Busca uma matrícula", description = "O aluno só acessa a própria matrícula.")
    public MatriculaResponse buscar(@PathVariable Long id, @UsuarioLogado UsuarioAutenticado usuario) {
        return matriculaService.buscar(id, usuario);
    }

    @PostMapping("/matriculas/{id}/troca-plano")
    @PreAuthorize("hasRole('RECEPCAO')")
    @Operation(summary = "Troca o plano da matrícula",
            description = "Encerra a matrícula atual, cancela as mensalidades futuras e cria a nova matrícula. "
                    + "O valor proporcional aos dias não usados do mês já pago vira desconto na primeira "
                    + "mensalidade da nova matrícula.")
    public TrocaPlanoResponse trocarPlano(@PathVariable Long id, @Valid @RequestBody TrocaPlanoRequest request) {
        return matriculaService.trocarPlano(id, request);
    }

    @PostMapping("/matriculas/{id}/cancelamento")
    @PreAuthorize("hasRole('RECEPCAO')")
    @Operation(summary = "Cancela a matrícula",
            description = "Cancela as mensalidades futuras. Em planos de 3 ou 12 meses, cobra multa de 10% sobre "
                    + "o valor das mensalidades canceladas.")
    public CancelamentoMatriculaResponse cancelar(@PathVariable Long id) {
        return matriculaService.cancelar(id);
    }

    @GetMapping("/alunos/{alunoId}/matriculas")
    @PreAuthorize("hasAnyRole('ALUNO','RECEPCAO')")
    @Operation(summary = "Histórico de matrículas do aluno")
    public List<MatriculaResponse> listarDoAluno(@PathVariable Long alunoId,
                                                 @UsuarioLogado UsuarioAutenticado usuario) {
        return matriculaService.listarPorAluno(alunoId, usuario);
    }

    @GetMapping("/alunos/{alunoId}/matriculas/ativa")
    @PreAuthorize("hasAnyRole('ALUNO','RECEPCAO')")
    @Operation(summary = "Matrícula ativa do aluno", description = "404 se o aluno não tem matrícula ativa.")
    public MatriculaResponse buscarAtiva(@PathVariable Long alunoId, @UsuarioLogado UsuarioAutenticado usuario) {
        return matriculaService.buscarAtiva(alunoId, usuario);
    }
}