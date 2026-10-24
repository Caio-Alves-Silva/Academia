package ifsp.edu.sistema_academia.dto.response;

import ifsp.edu.sistema_academia.model.Perfil;
import ifsp.edu.sistema_academia.model.Usuario;

/** Dados públicos de um usuário. Nunca inclui a senha (nem o hash). */
public record UsuarioResponse(Long id, String nome, String cpf, String email, Perfil perfil) {

    public static UsuarioResponse from(Usuario usuario) {
        return new UsuarioResponse(usuario.getId(), usuario.getNome(), usuario.getCpf(), usuario.getEmail(),
                usuario.getPerfil());
    }
}