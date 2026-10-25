package ifsp.edu.sistema_academia.repository;

import ifsp.edu.sistema_academia.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    boolean existsByCpf(String cpf);

    boolean existsByEmail(String email);

    Optional<Usuario> findByEmail(String email);
}