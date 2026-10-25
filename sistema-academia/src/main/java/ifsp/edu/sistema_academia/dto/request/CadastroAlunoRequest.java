package ifsp.edu.sistema_academia.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.br.CPF;

import java.time.LocalDate;

public record CadastroAlunoRequest(
        @NotBlank(message = "O nome é obrigatório")
        @Size(max = 120, message = "O nome deve ter no máximo 120 caracteres")
        @Schema(example = "Maria Silva")
        String nome,

        @NotBlank(message = "O CPF é obrigatório")
        @CPF(message = "CPF inválido")
        @Schema(example = "529.982.247-25", description = "Com ou sem pontuação")
        String cpf,

        @NotBlank(message = "O e-mail é obrigatório")
        @Email(message = "E-mail inválido")
        @Size(max = 160, message = "O e-mail deve ter no máximo 160 caracteres")
        @Schema(example = "maria@email.com")
        String email,

        @NotBlank(message = "A senha é obrigatória")
        @Size(min = 8, max = 72, message = "A senha deve ter entre 8 e 72 caracteres")
        @Schema(example = "senha1234")
        String senha,

        @Past(message = "A data de nascimento deve estar no passado")
        @Schema(example = "1995-04-20")
        LocalDate dataNascimento,

        @Size(max = 20, message = "O telefone deve ter no máximo 20 caracteres")
        @Schema(example = "(11) 99999-0000")
        String telefone,

        @Size(max = 255, message = "O objetivo deve ter no máximo 255 caracteres")
        @Schema(example = "Hipertrofia")
        String objetivo,

        @Size(max = 500, message = "As restrições médicas devem ter no máximo 500 caracteres")
        String restricoesMedicas) {

    /** Cadastro apenas com os dados de acesso (dados complementares do aluno são opcionais). */
    public CadastroAlunoRequest(String nome, String cpf, String email, String senha) {
        this(nome, cpf, email, senha, null, null, null, null);
    }
}
