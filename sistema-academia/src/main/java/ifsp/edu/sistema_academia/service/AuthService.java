package ifsp.edu.sistema_academia.service;

import ifsp.edu.sistema_academia.dto.request.CadastroAlunoRequest;
import ifsp.edu.sistema_academia.dto.request.LoginRequest;
import ifsp.edu.sistema_academia.dto.response.LoginResponse;
import ifsp.edu.sistema_academia.dto.response.UsuarioResponse;
import ifsp.edu.sistema_academia.exception.CredenciaisInvalidasException;
import ifsp.edu.sistema_academia.exception.DadoDuplicadoException;
import ifsp.edu.sistema_academia.exception.RegraNegocioException;
import ifsp.edu.sistema_academia.model.Perfil;
import ifsp.edu.sistema_academia.model.Usuario;
import ifsp.edu.sistema_academia.repository.UsuarioRepository;
import ifsp.edu.sistema_academia.security.JwtTokenService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;

/** RF01 (cadastro de aluno) e RF02 (login). */
@Service
public class AuthService {

    private static final int LIMITE_BYTES_SENHA = 72; // limite do BCrypt

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final Clock clock;
    /** Hash descartável: faz o login gastar o mesmo tempo com e-mail inexistente, sem vazar qual campo falhou. */
    private final String hashFalso;

    public AuthService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder,
                       JwtTokenService jwtTokenService, Clock clock) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
        this.clock = clock;
        this.hashFalso = passwordEncoder.encode("senha-inexistente");
    }

    @Transactional
    public UsuarioResponse cadastrarAluno(CadastroAlunoRequest request) {
        String cpf = normalizarCpf(request.cpf());
        String email = normalizarEmail(request.email());
        if (request.senha().getBytes(StandardCharsets.UTF_8).length > LIMITE_BYTES_SENHA) {
            throw new RegraNegocioException("A senha deve ter no máximo " + LIMITE_BYTES_SENHA + " bytes");
        }
        if (usuarioRepository.existsByCpf(cpf)) {
            throw new DadoDuplicadoException("CPF");
        }
        if (usuarioRepository.existsByEmail(email)) {
            throw new DadoDuplicadoException("E-mail");
        }
        Usuario aluno = new Usuario(request.nome().trim(), cpf, email, passwordEncoder.encode(request.senha()),
                Perfil.ALUNO, LocalDateTime.now(clock));
        return UsuarioResponse.from(usuarioRepository.saveAndFlush(aluno));
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        Optional<Usuario> usuario = usuarioRepository.findByEmail(normalizarEmail(request.email()));
        boolean senhaConfere = senhaConfere(request.senha(), usuario.map(Usuario::getSenhaHash).orElse(hashFalso));
        if (usuario.isEmpty() || !senhaConfere) {
            throw new CredenciaisInvalidasException();
        }
        JwtTokenService.TokenEmitido token = jwtTokenService.emitir(usuario.get());
        return new LoginResponse(token.token(), "Bearer", token.expiraEm(), usuario.get().getId(),
                usuario.get().getPerfil());
    }

    private boolean senhaConfere(String senha, String hash) {
        if (senha.getBytes(StandardCharsets.UTF_8).length > LIMITE_BYTES_SENHA) {
            return false;
        }
        return passwordEncoder.matches(senha, hash);
    }

    public static String normalizarCpf(String cpf) {
        return cpf.replaceAll("\\D", "");
    }

    public static String normalizarEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}