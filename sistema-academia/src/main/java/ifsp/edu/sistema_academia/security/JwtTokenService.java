package ifsp.edu.sistema_academia.security;

import ifsp.edu.sistema_academia.config.JwtProperties;
import ifsp.edu.sistema_academia.model.Perfil;
import ifsp.edu.sistema_academia.model.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/** Emite e valida tokens JWT (HS256, jjwt) com os claims {@code id}, {@code email} e {@code role}. */
@Service
public class JwtTokenService {

    public static final String CLAIM_ID = "id";
    public static final String CLAIM_EMAIL = "email";
    public static final String CLAIM_ROLE = "role";
    private static final String EMISSOR = "sistema-academia";

    private final SecretKey chave;
    private final JwtProperties properties;

    public JwtTokenService(JwtProperties properties) {
        this.properties = properties;
        this.chave = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public record TokenEmitido(String token, Instant expiraEm) {
    }

    public TokenEmitido emitir(Usuario usuario) {
        // O relógio do JWT é o real: o Clock da aplicação pode ser substituído em testes e não deve afetar a validade.
        Instant agora = Instant.now();
        Instant expiraEm = agora.plus(properties.expiracao());
        Instant emitidoEm = properties.expiracao().isNegative()
                ? expiraEm.minus(Duration.ofMinutes(1))
                : agora;
        String token = Jwts.builder()
                .issuer(EMISSOR)
                .subject(String.valueOf(usuario.getId()))
                .issuedAt(Date.from(emitidoEm))
                .expiration(Date.from(expiraEm))
                .claim(CLAIM_ID, usuario.getId())
                .claim(CLAIM_EMAIL, usuario.getEmail())
                .claim(CLAIM_ROLE, usuario.getPerfil().name())
                .signWith(chave, Jwts.SIG.HS256)
                .compact();
        return new TokenEmitido(token, expiraEm);
    }

    /**
     * Valida assinatura, emissor e validade do token e devolve o usuário identificado por ele.
     *
     * @throws BadCredentialsException se o token for inválido, adulterado ou estiver expirado
     */
    public UsuarioAutenticado validar(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(chave)
                    .requireIssuer(EMISSOR)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            Long id = ((Number) claims.get(CLAIM_ID)).longValue();
            Perfil perfil = Perfil.valueOf(claims.get(CLAIM_ROLE, String.class));
            return new UsuarioAutenticado(id, claims.get(CLAIM_EMAIL, String.class), perfil);
        } catch (JwtException | IllegalArgumentException | ClassCastException | NullPointerException e) {
            throw new BadCredentialsException("Token inválido", e);
        }
    }
}