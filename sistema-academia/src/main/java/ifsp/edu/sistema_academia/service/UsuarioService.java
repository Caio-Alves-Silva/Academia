package ifsp.edu.sistema_academia.service;

import ifsp.edu.sistema_academia.dto.response.UsuarioResponse;
import ifsp.edu.sistema_academia.exception.RecursoNaoEncontradoException;
import ifsp.edu.sistema_academia.model.Perfil;
import ifsp.edu.sistema_academia.repository.UsuarioRepository;
import ifsp.edu.sistema_academia.security.UsuarioAutenticado;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Consulta de alunos (a recepção e os instrutores precisam do ID do aluno para as demais operações). */
@Service
@Transactional(readOnly = true)
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public List<UsuarioResponse> listarAlunos(String nome) {
        List<ifsp.edu.sistema_academia.model.Usuario> alunos = (nome == null || nome.isBlank())
                ? usuarioRepository.findByPerfilOrderByNomeAsc(Perfil.ALUNO)
                : usuarioRepository.findByPerfilAndNomeContainingIgnoreCaseOrderByNomeAsc(Perfil.ALUNO, nome.trim());
        return alunos.stream().map(UsuarioResponse::from).toList();
    }

    public UsuarioResponse buscarAluno(Long id, UsuarioAutenticado solicitante) {
        solicitante.exigirAcessoAoAluno(id);
        return usuarioRepository.findByIdAndPerfil(id, Perfil.ALUNO)
                .map(UsuarioResponse::from)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Aluno", id));
    }
}