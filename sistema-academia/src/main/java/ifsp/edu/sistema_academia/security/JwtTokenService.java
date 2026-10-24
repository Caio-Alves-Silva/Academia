package ifsp.edu.sistema_academia.security;

import ifsp.edu.sistema_academia.config.JwtProperties;
import ifsp.edu.sistema_academia.model.Usuario;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;

/** Emite tokens JWT (HS256) que identificam o usuário e o perfil. */
@Service
public class JwtTokenService {

    public static final String CLAIM_PERFIL = "perfil";
    public static final String CLAIM_EMAIL = "email";

    private static final String EMISSOR = "sistema-academia";

    private final JwtEncoder encoder;
    private final JwtProperties properties;

    public JwtTokenService(JwtEncoder encoder, JwtProperties properties) {
        this.encoder = encoder;
        this.properties = properties;
    }

    public record TokenEmitido(String token, Instant expiraEm) {
    }

    public TokenEmitido emitir(Usuario usuario) {
        // O relógio do JWT é o real: o Clock da aplicação pode ser substituído em testes e não deve afetar a validade.
        Instant agora = Instant.now();
        Instant expiraEm = agora.plus(properties.expiracao());
        Instant emitidoEm = properties.expiracao().isNegative()
                ? expiraEm.minus(java.time.Duration.ofMinutes(1))
                : agora;
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(EMISSOR)
                .subject(String.valueOf(usuario.getId()))
                .issuedAt(emitidoEm)
                .expiresAt(expiraEm)
                .claim(CLAIM_EMAIL, usuario.getEmail())
                .claim(CLAIM_PERFIL, usuario.getPerfil().name())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new TokenEmitido(token, expiraEm);
    }
}