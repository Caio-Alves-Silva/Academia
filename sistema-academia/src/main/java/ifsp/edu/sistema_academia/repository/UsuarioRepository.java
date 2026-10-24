package ifsp.edu.sistema_academia.repository;

import ifsp.edu.sistema_academia.model.Perfil;
import ifsp.edu.sistema_academia.model.Usuario;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    boolean existsByCpf(String cpf);

    boolean existsByEmail(String email);

    Optional<Usuario> findByEmail(String email);

    Optional<Usuario> findByIdAndPerfil(Long id, Perfil perfil);

    List<Usuario> findByPerfilOrderByNomeAsc(Perfil perfil);

    List<Usuario> findByPerfilAndNomeContainingIgnoreCaseOrderByNomeAsc(Perfil perfil, String nome);

    /** Bloqueia a linha do usuário até o fim da transação, serializando operações concorrentes sobre o mesmo aluno. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from Usuario u where u.id = :id and u.perfil = :perfil")
    Optional<Usuario> findByIdAndPerfilForUpdate(@Param("id") Long id, @Param("perfil") Perfil perfil);
}