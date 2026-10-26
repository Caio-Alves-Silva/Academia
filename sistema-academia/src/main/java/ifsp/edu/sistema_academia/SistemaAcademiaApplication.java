package ifsp.edu.sistema_academia;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.net.InetSocketAddress;
import java.net.Socket;

@SpringBootApplication
public class SistemaAcademiaApplication {

	private static final Logger log = LoggerFactory.getLogger(SistemaAcademiaApplication.class);

	public static void main(String[] args) {
		SpringApplication app = new SpringApplication(SistemaAcademiaApplication.class);
		if (deveUsarFallbackH2(args)) {
			log.info("MySQL local (localhost:3306) não detectado: ativando perfil 'h2' (banco em memória) automaticamente.");
			app.setAdditionalProfiles("h2");
		}
		app.run(args);
	}

	private static boolean deveUsarFallbackH2(String[] args) {
		if (System.getProperty("spring.profiles.active") != null
				|| System.getenv("SPRING_PROFILES_ACTIVE") != null
				|| System.getenv("DB_URL") != null) {
			return false;
		}
		for (String arg : args) {
			if (arg != null && arg.contains("spring.profiles.active")) {
				return false;
			}
		}
		try (Socket socket = new Socket()) {
			socket.connect(new InetSocketAddress("localhost", 3306), 300);
			return false;
		} catch (Exception e) {
			return true;
		}
	}

}
