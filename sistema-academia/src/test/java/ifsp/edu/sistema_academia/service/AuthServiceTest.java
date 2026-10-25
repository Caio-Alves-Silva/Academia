package ifsp.edu.sistema_academia.service;

import ifsp.edu.sistema_academia.dto.request.CadastroAlunoRequest;
import ifsp.edu.sistema_academia.dto.request.LoginRequest;
import ifsp.edu.sistema_academia.dto.response.LoginResponse;
import ifsp.edu.sistema_academia.dto.response.UsuarioResponse;
import ifsp.edu.sistema_academia.exception.CredenciaisInvalidasException;
import ifsp.edu.sistema_academia.exception.DadoDuplicadoException;
import ifsp.edu.sistema_academia.exception.RegraNegocioException;
import ifsp.edu.sistema_academia.model.Aluno;
import ifsp.edu.sistema_academia.model.Perfil;
import ifsp.edu.sistema_academia.model.Usuario;
import ifsp.edu.sistema_academia.repository.UsuarioRepository;
import ifsp.edu.sistema_academia.security.JwtTokenService;
import ifsp.edu.sistema_academia.support.Fixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceTest {

    @Mock
    UsuarioRepository usuarioRepository;
    @Mock
    JwtTokenService jwtTokenService;

    final PasswordEncoder encoder = new BCryptPasswordEncoder(4);
    AuthService service;

    @BeforeEach
    void setUp() {
        service = new AuthService(usuarioRepository, encoder, jwtTokenService, Fixtures.relogio(Fixtures.AGORA));
        when(usuarioRepository.saveAndFlush(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private CadastroAlunoRequest cadastro() {
        return new CadastroAlunoRequest("  Maria Silva ", "529.982.247-25", " Maria@Email.COM ", "senha1234");
    }

    @Test
    @DisplayName("cadastro normaliza CPF e e-mail, grava o hash da senha e não devolve a senha")
    void cadastrar() {
        UsuarioResponse resposta = service.cadastrarAluno(cadastro());

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).saveAndFlush(captor.capture());
        Usuario salvo = captor.getValue();
        assertThat(salvo.getCpf()).isEqualTo("52998224725");
        assertThat(salvo.getEmail()).isEqualTo("maria@email.com");
        assertThat(salvo.getNome()).isEqualTo("Maria Silva");
        assertThat(salvo.getPerfil()).isEqualTo(Perfil.ALUNO);
        assertThat(salvo.getSenhaHash()).isNotEqualTo("senha1234");
        assertThat(encoder.matches("senha1234", salvo.getSenhaHash())).isTrue();
        assertThat(resposta.perfil()).isEqualTo(Perfil.ALUNO);
        assertThat(resposta.toString()).doesNotContain("senha1234").doesNotContain(salvo.getSenhaHash());
    }

    @Test
    void cpfDuplicado() {
        when(usuarioRepository.existsByCpf("52998224725")).thenReturn(true);

        assertThatThrownBy(() -> service.cadastrarAluno(cadastro()))
                .isInstanceOf(DadoDuplicadoException.class).hasMessageContaining("CPF");
        verify(usuarioRepository, never()).saveAndFlush(any());
    }

    @Test
    void emailDuplicado() {
        when(usuarioRepository.existsByEmail("maria@email.com")).thenReturn(true);

        assertThatThrownBy(() -> service.cadastrarAluno(cadastro()))
                .isInstanceOf(DadoDuplicadoException.class).hasMessageContaining("E-mail");
    }

    @Test
    void senhaMaiorQueOLimiteDoBcrypt() {
        CadastroAlunoRequest request = new CadastroAlunoRequest("Maria", "52998224725", "m@e.com",
                "ç".repeat(40)); // 40 caracteres, 80 bytes

        assertThatThrownBy(() -> service.cadastrarAluno(request)).isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void loginDevolveToken() {
        Usuario usuario = Fixtures.comId(new Aluno("Maria", "52998224725", "maria@email.com",
                encoder.encode("senha1234"), Fixtures.AGORA), 7L);
        when(usuarioRepository.findByEmail("maria@email.com")).thenReturn(Optional.of(usuario));
        Instant expira = Instant.now().plusSeconds(3600);
        when(jwtTokenService.emitir(usuario)).thenReturn(new JwtTokenService.TokenEmitido("tok", expira));

        LoginResponse resposta = service.login(new LoginRequest(" MARIA@email.com", "senha1234"));

        assertThat(resposta.token()).isEqualTo("tok");
        assertThat(resposta.tipo()).isEqualTo("Bearer");
        assertThat(resposta.usuarioId()).isEqualTo(7L);
        assertThat(resposta.perfil()).isEqualTo(Perfil.ALUNO);
        assertThat(resposta.expiraEm()).isEqualTo(expira);
    }

    @Test
    @DisplayName("credenciais inválidas não indicam qual campo está errado")
    void loginComCredenciaisInvalidas() {
        Usuario usuario = new Aluno("Maria", "52998224725", "maria@email.com", encoder.encode("senha1234"),
                Fixtures.AGORA);
        when(usuarioRepository.findByEmail("maria@email.com")).thenReturn(Optional.of(usuario));
        when(usuarioRepository.findByEmail("naoexiste@email.com")).thenReturn(Optional.empty());

        CredenciaisInvalidasException senhaErrada = org.junit.jupiter.api.Assertions.assertThrows(
                CredenciaisInvalidasException.class, () -> service.login(new LoginRequest("maria@email.com", "errada!!")));
        CredenciaisInvalidasException emailErrado = org.junit.jupiter.api.Assertions.assertThrows(
                CredenciaisInvalidasException.class,
                () -> service.login(new LoginRequest("naoexiste@email.com", "senha1234")));

        assertThat(senhaErrada.getMessage()).isEqualTo(emailErrado.getMessage()).isEqualTo("Credenciais inválidas");
        assertThatThrownBy(() -> service.login(new LoginRequest("maria@email.com", "ç".repeat(40))))
                .isInstanceOf(CredenciaisInvalidasException.class);
    }
}