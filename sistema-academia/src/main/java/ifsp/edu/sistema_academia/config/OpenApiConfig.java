package ifsp.edu.sistema_academia.config;

import ifsp.edu.sistema_academia.security.UsuarioLogado;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Documentação Swagger/OpenAPI com o esquema de autenticação Bearer JWT. */
@Configuration
public class OpenApiConfig {

    private static final String ESQUEMA_JWT = "bearerAuth";

    static {
        // O usuário logado vem do token; não é parâmetro da requisição.
        SpringDocUtils.getConfig().addAnnotationsToIgnore(UsuarioLogado.class);
    }

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("API Sistema Academia")
                        .version("1.0.0")
                        .description("""
                                Back-end da academia: planos e matrículas, mensalidades, aulas coletivas com lista \
                                de espera, check-in, fichas de treino e relatórios.

                                Faça login em `POST /api/auth/login`, copie o `token` e clique em **Authorize**."""))
                .components(new Components().addSecuritySchemes(ESQUEMA_JWT, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(ESQUEMA_JWT));
    }
}