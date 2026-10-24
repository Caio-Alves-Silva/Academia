package ifsp.edu.sistema_academia.exception;

import ifsp.edu.sistema_academia.dto.response.ErroResponse;
import ifsp.edu.sistema_academia.dto.response.ErroResponse.CampoInvalido;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.List;

/** Tratamento centralizado de erros: toda falha da API sai no formato de {@link ErroResponse}. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ErroResponse> naoEncontrado(RecursoNaoEncontradoException e, HttpServletRequest req) {
        return resposta(HttpStatus.NOT_FOUND, e.getMessage(), req, null);
    }

    @ExceptionHandler(ConflitoException.class)
    public ResponseEntity<ErroResponse> conflito(ConflitoException e, HttpServletRequest req) {
        return resposta(HttpStatus.CONFLICT, e.getMessage(), req, null);
    }

    @ExceptionHandler(RegraNegocioException.class)
    public ResponseEntity<ErroResponse> regraNegocio(RegraNegocioException e, HttpServletRequest req) {
        return resposta(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage(), req, null);
    }

    @ExceptionHandler(CredenciaisInvalidasException.class)
    public ResponseEntity<ErroResponse> credenciaisInvalidas(CredenciaisInvalidasException e,
                                                             HttpServletRequest req) {
        return resposta(HttpStatus.UNAUTHORIZED, e.getMessage(), req, null);
    }

    @ExceptionHandler(AcessoNegadoException.class)
    public ResponseEntity<ErroResponse> acessoNegado(AcessoNegadoException e, HttpServletRequest req) {
        return resposta(HttpStatus.FORBIDDEN, e.getMessage(), req, null);
    }

    /** Perfil sem permissão para o endpoint (@PreAuthorize). */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErroResponse> perfilSemPermissao(AccessDeniedException e, HttpServletRequest req) {
        return resposta(HttpStatus.FORBIDDEN, "Você não tem permissão para acessar este recurso", req, null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResponse> validacao(MethodArgumentNotValidException e, HttpServletRequest req) {
        List<CampoInvalido> campos = e.getBindingResult().getFieldErrors().stream()
                .map(f -> new CampoInvalido(f.getField(), f.getDefaultMessage()))
                .toList();
        return resposta(HttpStatus.BAD_REQUEST, "Dados da requisição inválidos", req, campos);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErroResponse> violacoes(ConstraintViolationException e, HttpServletRequest req) {
        List<CampoInvalido> campos = e.getConstraintViolations().stream()
                .map(v -> new CampoInvalido(v.getPropertyPath().toString(), v.getMessage()))
                .toList();
        return resposta(HttpStatus.BAD_REQUEST, "Dados da requisição inválidos", req, campos);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponse> corpoIlegivel(HttpMessageNotReadableException e, HttpServletRequest req) {
        return resposta(HttpStatus.BAD_REQUEST,
                "Corpo da requisição ausente, malformado ou com valores não reconhecidos (datas, enums, números)",
                req, null);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErroResponse> tipoIncorreto(MethodArgumentTypeMismatchException e,
                                                      HttpServletRequest req) {
        return resposta(HttpStatus.BAD_REQUEST, "Valor inválido para o parâmetro '" + e.getName() + "'", req, null);
    }

    /** Corrida em constraint única (ex.: dois cadastros simultâneos com o mesmo CPF). */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErroResponse> integridade(DataIntegrityViolationException e, HttpServletRequest req) {
        log.warn("Violação de integridade: {}", e.getMostSpecificCause().getMessage());
        return resposta(HttpStatus.CONFLICT, "A operação viola uma restrição de integridade dos dados", req, null);
    }

    /** Demais erros: exceções padrão do Spring MVC (404, 405, 415, parâmetro ausente...) e falhas inesperadas. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResponse> inesperado(Exception e, HttpServletRequest req) {
        if (e instanceof ErrorResponse mvc) {
            HttpStatus status = HttpStatus.valueOf(mvc.getStatusCode().value());
            String detalhe = mvc.getBody().getDetail();
            return resposta(status, detalhe == null ? status.getReasonPhrase() : detalhe, req, null);
        }
        log.error("Erro inesperado em {}", req.getRequestURI(), e);
        return resposta(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno do servidor", req, null);
    }

    private ResponseEntity<ErroResponse> resposta(HttpStatus status, String mensagem, HttpServletRequest req,
                                                  List<CampoInvalido> campos) {
        ErroResponse corpo = new ErroResponse(Instant.now(), status.value(), status.getReasonPhrase(), mensagem,
                req.getRequestURI(), campos);
        return ResponseEntity.status(status).body(corpo);
    }
}