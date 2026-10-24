package ifsp.edu.sistema_academia.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Configuração do JWT (prefixo {@code app.jwt}).
 *
 * @param secret    segredo HMAC-SHA256, com no mínimo 32 caracteres
 * @param expiracao validade do token
 */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String secret, Duration expiracao) {

    public JwtProperties {
        if (secret == null || secret.getBytes(java.nio.charset.StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("app.jwt.secret deve ter no mínimo 32 caracteres (256 bits)");
        }
        if (expiracao == null) {
            expiracao = Duration.ofHours(2);
        }
    }
}