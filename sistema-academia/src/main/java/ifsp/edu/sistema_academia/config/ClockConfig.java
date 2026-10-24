package ifsp.edu.sistema_academia.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/**
 * Relógio único da aplicação. Todas as regras de negócio dependentes de data/hora (vencimentos, semana corrente,
 * prazo de cancelamento) usam este bean, o que permite testá-las com datas controladas.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock(@Value("${app.timezone:America/Sao_Paulo}") String timezone) {
        return Clock.system(ZoneId.of(timezone));
    }
}