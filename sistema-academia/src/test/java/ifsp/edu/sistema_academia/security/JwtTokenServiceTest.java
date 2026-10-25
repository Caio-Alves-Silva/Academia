package ifsp.edu.sistema_academia.security;

import ifsp.edu.sistema_academia.config.JwtProperties;
import ifsp.edu.sistema_academia.config.SecurityConfig;
import ifsp.edu.sistema_academia.exception.RecursoNaoEncontradoException;
import ifsp.edu.sistema_academia.model.Perfil;
import ifsp.edu.sistema_academia.model.Usuario;
import ifsp.edu.sistema_academia.support.Fixtures;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenServiceTest {

    private static final JwtProperties PROPRIEDADES =
            new JwtProperties("segredo-de-teste-com-mais-de-32-caracteres!", Duration.ofMinutes(30));

    private final SecurityConfig config = new SecurityConfig();
    private final JwtEncoder encoder = config.jwtEncoder(PROPRIEDADES);
    private final JwtDecoder decoder = config.jwtDecoder(PROPRIEDADES);
    private final JwtTokenService service = new JwtTokenService(encoder, PROPRIEDADES);
    private final JwtUsuarioConverter converter = new JwtUsuarioConverter();

    private Usuario instrutor() {
        return Fixtures.comId(new Usuario("Carlos", "52998224725", "carlos@academia.com", "hash",
                Perfil.INSTRUTOR, Fixtures.AGORA), 42L);
    }

    @Test
    void tokenIdentificaOUsuarioEOPerfil() {
        JwtTokenService.TokenEmitido emitido = service.emitir(instrutor());

        Jwt jwt = decoder.decode(emitido.token());
        assertThat(jwt.getSubject()).isEqualTo("42");
        assertThat(jwt.getClaimAsString(JwtTokenService.CLAIM_PERFIL)).isEqualTo("INSTRUTOR");
        assertThat(jwt.getClaimAsString(JwtTokenService.CLAIM_EMAIL)).isEqualTo("carlos@academia.com");
        assertThat(jwt.getExpiresAt()).isAfter(Instant.now().plus(Duration.ofMinutes(29)));
        assertThat(emitido.expiraEm()).isAfter(Instant.now());
    }

    @Test
    void conversorMontaOPrincipalEAAuthority() {
        Jwt jwt = decoder.decode(service.emitir(instrutor()).token());

        AbstractAuthenticationToken autenticacao = converter.convert(jwt);

        assertThat(autenticacao.getPrincipal()).isEqualTo(new UsuarioAutenticado(42L, "carlos@academia.com", Perfil.INSTRUTOR));
        assertThat(autenticacao.getAuthorities()).extracting("authority").containsExactly("ROLE_INSTRUTOR");
        assertThat(autenticacao.isAuthenticated()).isTrue();
    }

    @Test
    void tokenAdulteradoOuDeOutroSegredoEhRejeitado() {
        String token = service.emitir(instrutor()).token();
        JwtDecoder outroSegredo = config.jwtDecoder(
                new JwtProperties("outro-segredo-com-mais-de-32-caracteres-!!", Duration.ofMinutes(5)));

        assertThatThrownBy(() -> outroSegredo.decode(token)).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> decoder.decode(token + "x")).isInstanceOf(RuntimeException.class);
    }

    @Test
    void tokenExpiradoEhRejeitado() {
        JwtTokenService expirado = new JwtTokenService(encoder,
                new JwtProperties(PROPRIEDADES.secret(), Duration.ofMinutes(-10)));

        assertThatThrownBy(() -> decoder.decode(expirado.emitir(instrutor()).token()))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void conversorRecusaTokenSemPerfilValido() {
        Jwt semPerfil = Jwt.withTokenValue("t").header("alg", "HS256").subject("1")
                .claim(JwtTokenService.CLAIM_PERFIL, "ADMIN").build();

        assertThatThrownBy(() -> converter.convert(semPerfil)).isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void segredoCurtoNaoEhAceito() {
        assertThatThrownBy(() -> new JwtProperties("curto", Duration.ofHours(1)))
                .isInstanceOf(IllegalStateException.class);
        assertThat(new JwtProperties("x".repeat(32), null).expiracao()).isEqualTo(Duration.ofHours(2));
        assertThat(RecursoNaoEncontradoException.de("Plano", 3L).getMessage()).contains("Plano").contains("3");
    }
}