package ifsp.edu.sistema_academia.controller;

import ifsp.edu.sistema_academia.dto.request.FichaTreinoRequest;
import ifsp.edu.sistema_academia.dto.response.FichaTreinoResponse;
import ifsp.edu.sistema_academia.security.UsuarioAutenticado;
import ifsp.edu.sistema_academia.security.UsuarioLogado;
import ifsp.edu.sistema_academia.service.FichaTreinoService;
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
@RequestMapping("/api/alunos/{alunoId}/fichas")
@Tag(name = "Fichas de treino", description = "Fichas de treino dos alunos (RF13)")
public class FichaTreinoController {

    private final FichaTreinoService fichaService;

    public FichaTreinoController(FichaTreinoService fichaService) {
        this.fichaService = fichaService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('INSTRUTOR')")
    @Operation(summary = "Cria a ficha de treino do aluno",
            description = "A ficha atual anterior (se houver) vai para o histórico.")
    public FichaTreinoResponse criar(@PathVariable Long alunoId, @Valid @RequestBody FichaTreinoRequest request,
                                     @UsuarioLogado UsuarioAutenticado usuario) {
        return fichaService.criar(alunoId, request, usuario);
    }

    @GetMapping("/atual")
    @PreAuthorize("hasAnyRole('ALUNO','INSTRUTOR')")
    @Operation(summary = "Ficha atual do aluno", description = "O aluno só acessa a própria ficha.")
    public FichaTreinoResponse buscarAtual(@PathVariable Long alunoId, @UsuarioLogado UsuarioAutenticado usuario) {
        return fichaService.buscarAtual(alunoId, usuario);
    }

    @GetMapping("/historico")
    @PreAuthorize("hasAnyRole('ALUNO','INSTRUTOR')")
    @Operation(summary = "Histórico de fichas do aluno", description = "Da mais recente para a mais antiga.")
    public List<FichaTreinoResponse> listarHistorico(@PathVariable Long alunoId,
                                                     @UsuarioLogado UsuarioAutenticado usuario) {
        return fichaService.listarHistorico(alunoId, usuario);
    }
}