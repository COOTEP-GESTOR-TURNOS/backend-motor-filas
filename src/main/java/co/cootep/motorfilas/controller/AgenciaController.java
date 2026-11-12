package co.cootep.motorfilas.controller;

import co.cootep.motorfilas.model.Modelos.CierreRequest;
import co.cootep.motorfilas.model.Modelos.CierreResponse;
import co.cootep.motorfilas.service.FilaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/agencias")
@Tag(name = "agencias")
public class AgenciaController {

    private final FilaService servicio;

    public AgenciaController(FilaService servicio) {
        this.servicio = servicio;
    }

    @PostMapping("/{agenciaId}/cierre")
    @Operation(summary = "Vacía en paralelo las filas de la agencia y devuelve los turnos retirados (RN-13)")
    public CierreResponse cerrar(@PathVariable UUID agenciaId, @Valid @RequestBody CierreRequest peticion) {
        return servicio.cerrarAgencia(agenciaId.toString(), peticion.servicioIds());
    }
}
