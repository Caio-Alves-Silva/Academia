package ifsp.edu.sistema_academia.controller;

import ifsp.edu.sistema_academia.dto.request.InscricaoRequest;
import ifsp.edu.sistema_academia.dto.response.CancelamentoInscricaoResponse;
import ifsp.edu.sistema_academia.dto.response.InscricaoResponse;
import ifsp.edu.sistema_academia.security.UsuarioAutenticado;
import ifsp.edu.sistema_academia.security.UsuarioLogado;
import ifsp.edu.sistema_academia.service.InscricaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Inscrições", description = "Inscrição em aulas, lista de espera e cancelamento (RF09, RF10)")
public class InscricaoController {

    private final InscricaoService inscricaoService;

    public InscricaoController(InscricaoService inscricaoService) {
        this.inscricaoService = inscricaoService;
    }

    @PostMapping("/aulas/{aulaId}/inscricoes")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ALUNO')")
    @Operation(summary = "Inscreve o aluno logado em uma aula",
            description = "A data deve ser de uma ocorrência da semana corrente. A modalidade precisa estar no plano "
                    + "e o limite semanal não pode ter sido atingido. Sem vaga, o aluno entra na lista de espera "
                    + "(status LISTA_ESPERA), por ordem de chegada.")
    public InscricaoResponse inscrever(@PathVariable Long aulaId, @Valid @RequestBody InscricaoRequest request,
                                       @UsuarioLogado UsuarioAutenticado usuario) {
        return inscricaoService.inscrever(aulaId, request, usuario);
    }

    @PostMapping("/inscricoes/{id}/cancelamento")
    @PreAuthorize("hasAnyRole('ALUNO','RECEPCAO')")
    @Operation(summary = "Cancela uma inscrição",
            description = "Permitido até 2 horas antes da aula. A vaga vai automaticamente para o primeiro da "
                    + "lista de espera, na mesma transação.")
    public CancelamentoInscricaoResponse cancelar(@PathVariable Long id, @UsuarioLogado UsuarioAutenticado usuario) {
        return inscricaoService.cancelar(id, usuario);
    }

    @GetMapping("/alunos/{alunoId}/inscricoes")
    @PreAuthorize("hasAnyRole('ALUNO','RECEPCAO')")
    @Operation(summary = "Lista as inscrições do aluno", description = "O aluno só acessa as próprias inscrições.")
    public List<InscricaoResponse> listarDoAluno(@PathVariable Long alunoId,
                                                 @UsuarioLogado UsuarioAutenticado usuario) {
        return inscricaoService.listarPorAluno(alunoId, usuario);
    }

    @GetMapping("/aulas/{aulaId}/inscricoes")
    @PreAuthorize("hasAnyRole('RECEPCAO','INSTRUTOR')")
    @Operation(summary = "Lista confirmados e lista de espera de uma ocorrência da aula",
            description = "Em ordem de chegada. Recepção e instrutor.")
    public List<InscricaoResponse> listarDaAula(@PathVariable Long aulaId,
                                                @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                                                LocalDate data) {
        return inscricaoService.listarPorOcorrencia(aulaId, data);
    }
}