package ifsp.edu.sistema_academia.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/** Substitui o relógio da aplicação por um {@link MutableClock} nos testes funcionais. */
@TestConfiguration(proxyBeanMethods = false)
public class TestClockConfig {

    @Bean
    @Primary
    public MutableClock relogioDeTeste() {
        return new MutableClock(Fixtures.ZONA);
    }
}