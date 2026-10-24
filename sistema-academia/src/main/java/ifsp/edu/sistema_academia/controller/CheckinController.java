package ifsp.edu.sistema_academia.controller;

import ifsp.edu.sistema_academia.dto.request.CheckinRequest;
import ifsp.edu.sistema_academia.dto.response.CheckinResponse;
import ifsp.edu.sistema_academia.service.CheckinService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/checkins")
@Tag(name = "Check-in", description = "Entrada na academia (RF12)")
public class CheckinController {

    private final CheckinService checkinService;

    public CheckinController(CheckinService checkinService) {
        this.checkinService = checkinService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('RECEPCAO')")
    @Operation(summary = "Registra a entrada de um aluno",
            description = "Aluno bloqueado por inadimplência ou sem matrícula ativa é recusado (422).")
    public CheckinResponse registrar(@Valid @RequestBody CheckinRequest request) {
        return checkinService.registrar(request);
    }
}