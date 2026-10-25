package ifsp.edu.sistema_academia.service;

import ifsp.edu.sistema_academia.dto.response.UsuarioResponse;
import ifsp.edu.sistema_academia.exception.RecursoNaoEncontradoException;
import ifsp.edu.sistema_academia.model.Aluno;
import ifsp.edu.sistema_academia.repository.AlunoRepository;
import ifsp.edu.sistema_academia.security.UsuarioAutenticado;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Consulta de alunos (a recepção e os instrutores precisam do ID do aluno para as demais operações). */
@Service
@Transactional(readOnly = true)
public class UsuarioService {

    private final AlunoRepository alunoRepository;

    public UsuarioService(AlunoRepository alunoRepository) {
        this.alunoRepository = alunoRepository;
    }

    public List<UsuarioResponse> listarAlunos(String nome) {
        List<Aluno> alunos = (nome == null || nome.isBlank())
                ? alunoRepository.findAllByOrderByNomeAsc()
                : alunoRepository.findByNomeContainingIgnoreCaseOrderByNomeAsc(nome.trim());
        return alunos.stream().map(UsuarioResponse::from).toList();
    }

    public UsuarioResponse buscarAluno(Long id, UsuarioAutenticado solicitante) {
        solicitante.exigirAcessoAoAluno(id);
        return alunoRepository.findById(id)
                .map(UsuarioResponse::from)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Aluno", id));
    }
}