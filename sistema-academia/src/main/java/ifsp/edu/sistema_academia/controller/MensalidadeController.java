package ifsp.edu.sistema_academia.controller;

import ifsp.edu.sistema_academia.dto.response.MensalidadeResponse;
import ifsp.edu.sistema_academia.model.StatusMensalidade;
import ifsp.edu.sistema_academia.security.UsuarioAutenticado;
import ifsp.edu.sistema_academia.security.UsuarioLogado;
import ifsp.edu.sistema_academia.service.MensalidadeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Mensalidades", description = "Consulta e pagamento de mensalidades (RF05)")
public class MensalidadeController {

    private final MensalidadeService mensalidadeService;

    public MensalidadeController(MensalidadeService mensalidadeService) {
        this.mensalidadeService = mensalidadeService;
    }

    @GetMapping("/alunos/{alunoId}/mensalidades")
    @PreAuthorize("hasAnyRole('ALUNO','RECEPCAO')")
    @Operation(summary = "Lista as mensalidades do aluno", description = "Filtro opcional por status.")
    public List<MensalidadeResponse> listarDoAluno(@PathVariable Long alunoId,
                                                   @RequestParam(required = false) StatusMensalidade status,
                                                   @UsuarioLogado UsuarioAutenticado usuario) {
        return mensalidadeService.listarPorAluno(alunoId, status, usuario);
    }

    @GetMapping("/mensalidades/{id}")
    @PreAuthorize("hasAnyRole('ALUNO','RECEPCAO')")
    @Operation(summary = "Busca uma mensalidade")
    public MensalidadeResponse buscar(@PathVariable Long id, @UsuarioLogado UsuarioAutenticado usuario) {
        return mensalidadeService.buscar(id, usuario);
    }

    @PostMapping("/mensalidades/{id}/pagamento")
    @PreAuthorize("hasAnyRole('ALUNO','RECEPCAO')")
    @Operation(summary = "Paga a mensalidade",
            description = "Pagamento após o vencimento tem multa de 2%. Aluno com mensalidade vencida há mais de "
                    + "5 dias fica bloqueado (sem check-in e sem inscrição em aulas).")
    public MensalidadeResponse pagar(@PathVariable Long id, @UsuarioLogado UsuarioAutenticado usuario) {
        return mensalidadeService.pagar(id, usuario);
    }
}