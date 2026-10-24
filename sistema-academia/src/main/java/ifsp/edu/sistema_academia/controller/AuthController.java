package ifsp.edu.sistema_academia.controller;

import ifsp.edu.sistema_academia.dto.request.CadastroAlunoRequest;
import ifsp.edu.sistema_academia.dto.request.LoginRequest;
import ifsp.edu.sistema_academia.dto.response.LoginResponse;
import ifsp.edu.sistema_academia.dto.response.UsuarioResponse;
import ifsp.edu.sistema_academia.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticação", description = "Cadastro de aluno e login (RF01, RF02)")
@SecurityRequirements
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/cadastro")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastra um aluno", description = "CPF e e-mail únicos; senha com no mínimo 8 caracteres. "
            + "A resposta nunca devolve a senha.")
    public UsuarioResponse cadastrar(@Valid @RequestBody CadastroAlunoRequest request) {
        return authService.cadastrarAluno(request);
    }

    @PostMapping("/login")
    @Operation(summary = "Autentica e devolve o token JWT",
            description = "Credenciais inválidas não indicam qual campo está errado.")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
}