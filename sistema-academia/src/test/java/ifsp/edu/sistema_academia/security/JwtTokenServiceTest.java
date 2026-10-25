package ifsp.edu.sistema_academia.security;

import ifsp.edu.sistema_academia.config.JwtProperties;
import ifsp.edu.sistema_academia.exception.RecursoNaoEncontradoException;
import ifsp.edu.sistema_academia.model.Instrutor;
import ifsp.edu.sistema_academia.model.Perfil;
import ifsp.edu.sistema_academia.support.Fixtures;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenServiceTest {

    private static final JwtProperties PROPRIEDADES =
            new JwtProperties("segredo-de-teste-com-mais-de-32-caracteres!", Duration.ofMinutes(30));

    private final JwtTokenService service = new JwtTokenService(PROPRIEDADES);

    private Instrutor instrutor() {
        return Fixtures.comId(new Instrutor("Carlos", "52998224725", "carlos@academia.com", "hash",
                Fixtures.AGORA), 42L);
    }

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void tokenTemClaimsDeIdEmailERole() {
        JwtTokenService.TokenEmitido emitido = service.emitir(instrutor());

        SecretKey chave = Keys.hmacShaKeyFor(PROPRIEDADES.secret().getBytes(StandardCharsets.UTF_8));
        Claims claims = Jwts.parser().verifyWith(chave).build().parseSignedClaims(emitido.token()).getPayload();
        assertThat(claims.getSubject()).isEqualTo("42");
        assertThat(((Number) claims.get(JwtTokenService.CLAIM_ID)).longValue()).isEqualTo(42L);
        assertThat(claims.get(JwtTokenService.CLAIM_ROLE, String.class)).isEqualTo("INSTRUTOR");
        assertThat(claims.get(JwtTokenService.CLAIM_EMAIL, String.class)).isEqualTo("carlos@academia.com");
        assertThat(claims.getExpiration().toInstant()).isAfter(Instant.now().plus(Duration.ofMinutes(29)));
        assertThat(emitido.expiraEm()).isAfter(Instant.now());
    }

    @Test
    void validarDevolveOPrincipal() {
        UsuarioAutenticado principal = service.validar(service.emitir(instrutor()).token());

        assertThat(principal).isEqualTo(new UsuarioAutenticado(42L, "carlos@academia.com", Perfil.INSTRUTOR));
    }

    @Test
    void tokenAdulteradoOuDeOutroSegredoEhRejeitado() {
        String token = service.emitir(instrutor()).token();
        JwtTokenService outroSegredo = new JwtTokenService(
                new JwtProperties("outro-segredo-com-mais-de-32-caracteres-!!", Duration.ofMinutes(5)));

        assertThatThrownBy(() -> outroSegredo.validar(token)).isInstanceOf(BadCredentialsException.class);
        assertThatThrownBy(() -> service.validar(token + "x")).isInstanceOf(BadCredentialsException.class);
        assertThatThrownBy(() -> service.validar("lixo")).isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void tokenExpiradoEhRejeitado() {
        JwtTokenService expirado = new JwtTokenService(
                new JwtProperties(PROPRIEDADES.secret(), Duration.ofMinutes(-10)));

        assertThatThrownBy(() -> service.validar(expirado.emitir(instrutor()).token()))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void tokenSemRoleValidaEhRejeitado() {
        SecretKey chave = Keys.hmacShaKeyFor(PROPRIEDADES.secret().getBytes(StandardCharsets.UTF_8));
        String semRole = Jwts.builder().issuer("sistema-academia").subject("1").claim(JwtTokenService.CLAIM_ID, 1)
                .claim(JwtTokenService.CLAIM_ROLE, "ADMIN")
                .expiration(Date.from(Instant.now().plusSeconds(60))).signWith(chave).compact();

        assertThatThrownBy(() -> service.validar(semRole)).isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void filtroAutenticaComTokenValido() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + service.emitir(instrutor()).token());
        MockFilterChain chain = new MockFilterChain();

        new JwtAuthenticationFilter(service).doFilter(request, new MockHttpServletResponse(), chain);

        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        assertThat(autenticacao.getPrincipal())
                .isEqualTo(new UsuarioAutenticado(42L, "carlos@academia.com", Perfil.INSTRUTOR));
        assertThat(autenticacao.getAuthorities()).extracting("authority").containsExactly("ROLE_INSTRUTOR");
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    void filtroDeixaAnonimoComTokenInvalidoOuAusente() throws ServletException, IOException {
        MockHttpServletRequest invalido = new MockHttpServletRequest();
        invalido.addHeader("Authorization", "Bearer lixo");
        new JwtAuthenticationFilter(service).doFilter(invalido, new MockHttpServletResponse(), new MockFilterChain());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();

        new JwtAuthenticationFilter(service).doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(),
                new MockFilterChain());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void segredoCurtoNaoEhAceito() {
        assertThatThrownBy(() -> new JwtProperties("curto", Duration.ofHours(1)))
                .isInstanceOf(IllegalStateException.class);
        assertThat(new JwtProperties("x".repeat(32), null).expiracao()).isEqualTo(Duration.ofHours(2));
        assertThat(RecursoNaoEncontradoException.de("Plano", 3L).getMessage()).contains("Plano").contains("3");
    }
}
