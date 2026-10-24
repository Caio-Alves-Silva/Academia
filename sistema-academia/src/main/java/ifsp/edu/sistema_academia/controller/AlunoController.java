package ifsp.edu.sistema_academia.controller;

import ifsp.edu.sistema_academia.dto.response.UsuarioResponse;
import ifsp.edu.sistema_academia.security.UsuarioAutenticado;
import ifsp.edu.sistema_academia.security.UsuarioLogado;
import ifsp.edu.sistema_academia.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/alunos")
@Tag(name = "Alunos", description = "Consulta de alunos")
public class AlunoController {

    private final UsuarioService usuarioService;

    public AlunoController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('RECEPCAO','INSTRUTOR')")
    @Operation(summary = "Lista alunos", description = "Filtro opcional por parte do nome. Recepção e instrutor.")
    public List<UsuarioResponse> listar(@RequestParam(required = false) String nome) {
        return usuarioService.listarAlunos(nome);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ALUNO','RECEPCAO','INSTRUTOR')")
    @Operation(summary = "Busca um aluno", description = "O aluno só consulta a si mesmo.")
    public UsuarioResponse buscar(@PathVariable Long id, @UsuarioLogado UsuarioAutenticado usuario) {
        return usuarioService.buscarAluno(id, usuario);
    }
}