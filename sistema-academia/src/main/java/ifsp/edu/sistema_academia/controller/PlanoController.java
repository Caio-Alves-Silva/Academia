package ifsp.edu.sistema_academia.controller;

import ifsp.edu.sistema_academia.dto.request.PlanoRequest;
import ifsp.edu.sistema_academia.dto.response.PlanoResponse;
import ifsp.edu.sistema_academia.service.PlanoService;
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
@RequestMapping("/api/planos")
@Tag(name = "Planos", description = "Ofertas da academia (RF03)")
public class PlanoController {

    private final PlanoService planoService;

    public PlanoController(PlanoService planoService) {
        this.planoService = planoService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('RECEPCAO')")
    @Operation(summary = "Cadastra um plano", description = "Duração de 1, 3 ou 12 meses. Somente recepção.")
    public PlanoResponse criar(@Valid @RequestBody PlanoRequest request) {
        return planoService.criar(request);
    }

    @GetMapping
    @Operation(summary = "Lista os planos", description = "Qualquer usuário autenticado.")
    public List<PlanoResponse> listar() {
        return planoService.listar();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca um plano")
    public PlanoResponse buscar(@PathVariable Long id) {
        return planoService.buscar(id);
    }
}