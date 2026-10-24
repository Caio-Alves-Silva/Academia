package ifsp.edu.sistema_academia.security;

import ifsp.edu.sistema_academia.model.Perfil;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Converte o JWT validado em uma autenticação cujo principal é o {@link UsuarioAutenticado} e cuja authority é
 * {@code ROLE_<PERFIL>}.
 */
@Component
public class JwtUsuarioConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Long id;
        Perfil perfil;
        try {
            id = Long.valueOf(jwt.getSubject());
            perfil = Perfil.valueOf(jwt.getClaimAsString(JwtTokenService.CLAIM_PERFIL));
        } catch (RuntimeException e) {
            throw new BadCredentialsException("Token inválido", e);
        }
        UsuarioAutenticado principal =
                new UsuarioAutenticado(id, jwt.getClaimAsString(JwtTokenService.CLAIM_EMAIL), perfil);
        return UsernamePasswordAuthenticationToken.authenticated(
                principal, jwt, List.of(new SimpleGrantedAuthority(perfil.authority())));
    }
}