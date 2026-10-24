package ifsp.edu.sistema_academia.controller;

import ifsp.edu.sistema_academia.dto.request.AulaRequest;
import ifsp.edu.sistema_academia.dto.request.PresencaRequest;
import ifsp.edu.sistema_academia.dto.response.AulaResponse;
import ifsp.edu.sistema_academia.dto.response.PresencaResponse;
import ifsp.edu.sistema_academia.model.Modalidade;
import ifsp.edu.sistema_academia.security.UsuarioAutenticado;
import ifsp.edu.sistema_academia.security.UsuarioLogado;
import ifsp.edu.sistema_academia.service.AulaService;
import ifsp.edu.sistema_academia.service.PresencaService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/aulas")
@Tag(name = "Aulas coletivas", description = "Cadastro de aulas e registro de presença (RF08, RF11)")
public class AulaController {

    private final AulaService aulaService;
    private final PresencaService presencaService;

    public AulaController(AulaService aulaService, PresencaService presencaService) {
        this.aulaService = aulaService;
        this.presencaService = presencaService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('RECEPCAO')")
    @Operation(summary = "Cadastra uma aula coletiva",
            description = "Um instrutor não pode ter duas aulas com horários sobrepostos.")
    public AulaResponse criar(@Valid @RequestBody AulaRequest request) {
        return aulaService.criar(request);
    }

    @GetMapping
    @Operation(summary = "Lista as aulas", description = "Filtro opcional por modalidade.")
    public List<AulaResponse> listar(@RequestParam(required = false) Modalidade modalidade) {
        return aulaService.listar(modalidade);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca uma aula")
    public AulaResponse buscar(@PathVariable Long id) {
        return aulaService.buscar(id);
    }

    @PostMapping("/{id}/presencas")
    @PreAuthorize("hasRole('INSTRUTOR')")
    @Operation(summary = "Registra a lista de presença de uma ocorrência da aula",
            description = "Somente o instrutor da aula. Informe os alunos presentes: os demais inscritos "
                    + "confirmados recebem falta. Duas faltas na mesma semana impedem a inscrição na semana seguinte.")
    public PresencaResponse registrarPresenca(@PathVariable Long id, @Valid @RequestBody PresencaRequest request,
                                              @UsuarioLogado UsuarioAutenticado usuario) {
        return presencaService.registrar(id, request, usuario);
    }
}