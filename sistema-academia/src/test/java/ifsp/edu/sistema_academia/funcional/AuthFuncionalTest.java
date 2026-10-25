package ifsp.edu.sistema_academia.funcional;

import ifsp.edu.sistema_academia.model.Usuario;
import ifsp.edu.sistema_academia.support.FuncionalTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** RF01 (cadastro), RF02 (login) e as regras de autenticação/autorização do Spring Security com JWT. */
class AuthFuncionalTest extends FuncionalTestBase {

    private ResultActions cadastrar(String nome, String cpf, String email, String senha) throws Exception {
        return postar("/api/auth/cadastro", null, mapa("nome", nome, "cpf", cpf, "email", email, "senha", senha));
    }

    @Test
    @DisplayName("RF01: cadastra aluno, normaliza CPF/e-mail e nunca devolve a senha")
    void cadastroDeAluno() throws Exception {
        String cpf = cpfValido();

        ResultActions resposta = cadastrar("Maria Silva", cpf, "Maria.Silva@Email.com", SENHA)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.nome").value("Maria Silva"))
                .andExpect(jsonPath("$.email").value("maria.silva@email.com"))
                .andExpect(jsonPath("$.perfil").value("ALUNO"))
                .andExpect(jsonPath("$.senha").doesNotExist())
                .andExpect(jsonPath("$.senhaHash").doesNotExist());

        assertThat(corpo(resposta)).doesNotContain(SENHA).doesNotContain("$2a$");
        Usuario salvo = usuarioRepository.findByEmail("maria.silva@email.com").orElseThrow();
        assertThat(salvo.getCpf()).isEqualTo(cpf);
        assertThat(salvo.getSenhaHash()).isNotEqualTo(SENHA).startsWith("$2");
    }

    @Test
    @DisplayName("RF01: CPF e e-mail aceitos com pontuação; ambos são únicos")
    void cpfEEmailUnicos() throws Exception {
        String cpf = cpfValido();
        String formatado = cpf.substring(0, 3) + "." + cpf.substring(3, 6) + "." + cpf.substring(6, 9) + "-"
                + cpf.substring(9);
        cadastrar("Maria", formatado, "maria@email.com", SENHA).andExpect(status().isCreated());

        cadastrar("Outra", cpf, "outra@email.com", SENHA)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem", containsString("CPF")));
        cadastrar("Outra", cpfValido(), "MARIA@email.com", SENHA)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem", containsString("E-mail")));
        assertThat(usuarioRepository.count()).isEqualTo(4); // 3 da carga inicial + Maria
    }

    @Test
    @DisplayName("RF01: validação do cadastro (senha com mínimo de 8 caracteres, CPF, e-mail e nome)")
    void validacaoDoCadastro() throws Exception {
        cadastrar("Maria", cpfValido(), "maria@email.com", "1234567")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[*].campo", hasItem("senha")));
        cadastrar("Maria", cpfValido(), "maria@email.com", "12345678").andExpect(status().isCreated());

        cadastrar("Maria", "111.111.111-11", "m2@email.com", SENHA)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[*].campo", hasItem("cpf")));
        cadastrar("Maria", cpfValido(), "email-invalido", SENHA)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[*].campo", hasItem("email")));
        cadastrar(" ", cpfValido(), "m3@email.com", SENHA)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[*].campo", hasItem("nome")))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.caminho").value("/api/auth/cadastro"));
        postar("/api/auth/cadastro", null, "{ json quebrado")
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("RF02: login devolve um JWT que identifica o usuário e o perfil")
    void loginDevolveJwt() throws Exception {
        Aluno aluno = novoAluno();

        ResultActions resposta = postar("/api/auth/login", null, mapa("email", aluno.email(), "senha", SENHA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andExpect(jsonPath("$.usuarioId").value(aluno.id()))
                .andExpect(jsonPath("$.perfil").value("ALUNO"))
                .andExpect(jsonPath("$.expiraEm", notNullValue()));

        String token = ler(resposta, "$.token");
        assertThat(token.split("\\.")).hasSize(3); // header.payload.assinatura
        consultar("/api/alunos/" + aluno.id(), token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(aluno.id()));
    }

    @Test
    @DisplayName("RF02: contas de recepção e instrutor da carga inicial conseguem entrar")
    void contasDaCargaInicial() throws Exception {
        postar("/api/auth/login", null, mapa("email", "recepcao@academia.com", "senha", "Recepcao@123"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.perfil").value("RECEPCAO"));
        postar("/api/auth/login", null, mapa("email", "instrutor@academia.com", "senha", "Instrutor@123"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.perfil").value("INSTRUTOR"));
    }

    @Test
    @DisplayName("RF02: credenciais inválidas não indicam qual campo está errado")
    void credenciaisInvalidas() throws Exception {
        Aluno aluno = novoAluno();

        String senhaErrada = corpo(postar("/api/auth/login", null, mapa("email", aluno.email(), "senha", "errada123"))
                .andExpect(status().isUnauthorized()));
        String emailErrado = corpo(postar("/api/auth/login", null, mapa("email", "nao@existe.com", "senha", SENHA))
                .andExpect(status().isUnauthorized()));

        String mensagem1 = ler(postar("/api/auth/login", null, mapa("email", aluno.email(), "senha", "errada123")),
                "$.mensagem");
        String mensagem2 = ler(postar("/api/auth/login", null, mapa("email", "nao@existe.com", "senha", SENHA)),
                "$.mensagem");
        assertThat(mensagem1).isEqualTo(mensagem2).isEqualTo("Credenciais inválidas");
        assertThat(senhaErrada).doesNotContain("token");
        assertThat(emailErrado).doesNotContain("token");
        postar("/api/auth/login", null, mapa("email", "", "senha", ""))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("endpoints protegidos exigem token válido")
    void exigeToken() throws Exception {
        consultar("/api/planos", null)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.mensagem", notNullValue()));
        consultar("/api/planos", "token.invalido.aqui").andExpect(status().isUnauthorized());
        consultar("/api/planos", tokenRecepcao()).andExpect(status().isOk());
    }

    @Test
    @DisplayName("token expirado é rejeitado")
    void tokenExpirado() throws Exception {
        // Token emitido por um serviço com validade negativa, assinado com o mesmo segredo da aplicação
        var config = new ifsp.edu.sistema_academia.config.SecurityConfig();
        var propriedades = new ifsp.edu.sistema_academia.config.JwtProperties(
                "segredo-de-teste-com-mais-de-32-caracteres-xyz", java.time.Duration.ofMinutes(-5));
        var servico = new ifsp.edu.sistema_academia.security.JwtTokenService(config.jwtEncoder(propriedades),
                propriedades);
        String expirado = servico.emitir(usuarioRepository.findByEmail("recepcao@academia.com").orElseThrow()).token();

        consultar("/api/planos", expirado).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("perfil sem permissão recebe 403 em JSON")
    void perfilSemPermissao() throws Exception {
        Aluno aluno = novoAluno();
        Object plano = mapa("nome", "X", "duracaoMeses", 1, "valorMensal", 10, "modalidades",
                java.util.List.of("YOGA"), "limiteAulasSemana", 1);

        postar("/api/planos", aluno.token(), plano)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.erro").value("Forbidden"));
        postar("/api/planos", tokenInstrutor(), plano).andExpect(status().isForbidden());
        postar("/api/planos", tokenRecepcao(), plano).andExpect(status().isCreated());
        consultar("/api/alunos", aluno.token()).andExpect(status().isForbidden());
        consultar("/api/relatorios/inadimplentes", tokenInstrutor()).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("o aluno só consulta os próprios dados")
    void alunoNaoAcessaDadosDeOutro() throws Exception {
        Aluno a = novoAluno();
        Aluno b = novoAluno();

        consultar("/api/alunos/" + a.id(), a.token()).andExpect(status().isOk());
        consultar("/api/alunos/" + a.id(), b.token())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensagem", not(nullValue())));
        consultar("/api/alunos/" + a.id(), tokenRecepcao()).andExpect(status().isOk());
        consultar("/api/alunos/999999", tokenRecepcao()).andExpect(status().isNotFound());
        consultar("/api/alunos?nome=aluno", tokenRecepcao())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("documentação Swagger/OpenAPI é pública e descreve a autenticação Bearer")
    void swagger() throws Exception {
        consultar("/v3/api-docs", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("API Sistema Academia"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
                .andExpect(jsonPath("$.paths['/api/auth/login']").exists())
                .andExpect(jsonPath("$.paths['/api/matriculas']").exists())
                .andExpect(jsonPath("$.paths['/api/relatorios/inadimplentes']").exists());
    }

    @Test
    @DisplayName("erros de roteamento seguem o formato padrão de erro")
    void errosPadronizados() throws Exception {
        consultar("/api/nao-existe", tokenRecepcao())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.caminho").value("/api/nao-existe"));
        consultar("/api/alunos/abc", tokenRecepcao())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem", containsString("id")));
    }
}
