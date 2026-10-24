package ifsp.edu.sistema_academia.dto.response;

import ifsp.edu.sistema_academia.model.Perfil;

import java.time.Instant;

public record LoginResponse(String token, String tipo, Instant expiraEm, Long usuarioId, Perfil perfil) {
}