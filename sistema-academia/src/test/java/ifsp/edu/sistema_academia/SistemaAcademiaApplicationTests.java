package ifsp.edu.sistema_academia;

import ifsp.edu.sistema_academia.support.FuncionalTestBase;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Smoke test: a aplicação sobe com a configuração de teste e a carga inicial cria as contas da equipe. */
class SistemaAcademiaApplicationTests extends FuncionalTestBase {

	@Test
	void contextLoads() {
		assertThat(usuarioRepository.findByEmail("recepcao@academia.com")).isPresent();
		assertThat(usuarioRepository.findByEmail("instrutor@academia.com")).isPresent();
		assertThat(usuarioRepository.findByEmail("instrutor2@academia.com")).isPresent();
	}

}
