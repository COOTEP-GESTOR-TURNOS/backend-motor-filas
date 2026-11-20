package co.cootep.motorfilas.controller;

import co.cootep.motorfilas.model.Modelos.EncolarRequest;
import co.cootep.motorfilas.model.Modelos.EncolarResponse;
import co.cootep.motorfilas.model.Modelos.EstadoFilaResponse;
import co.cootep.motorfilas.model.Modelos.PaginaFilaResponse;
import co.cootep.motorfilas.model.Modelos.PosicionResponse;
import co.cootep.motorfilas.model.Modelos.SiguienteRequest;
import co.cootep.motorfilas.model.Modelos.SiguienteResponse;
import co.cootep.motorfilas.service.FilaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Controlador de filas. La fila se identifica como agenciaId:servicioId (dos UUID). */
@RestController
@RequestMapping("/api/v1/filas")
@Tag(name = "filas", description = "Operaciones atómicas sobre las filas en Redis")
public class FilaController {

    static final String PATRON_FILA = "^[0-9a-fA-F-]{36}:[0-9a-fA-F-]{36}$";

    private final FilaService servicio;

    public FilaController(FilaService servicio) {
        this.servicio = servicio;
    }

    @PostMapping("/{filaId}/turnos")
    @Operation(summary = "Encola un turno; idempotente por turnoId")
    public EncolarResponse encolar(@PathVariable @Pattern(regexp = PATRON_FILA) String filaId,
                                   @Valid @RequestBody EncolarRequest peticion) {
        return servicio.encolar(filaId, peticion);
    }

    @GetMapping("/{filaId}/turnos")
    @Operation(summary = "Consulta una página de turnos en espera según la regla K")
    public PaginaFilaResponse listar(@PathVariable @Pattern(regexp = PATRON_FILA) String filaId,
                                    @RequestParam(defaultValue = "2") @Min(1) @Max(10) int k,
                                    @RequestParam(defaultValue = "1") @Min(1) int pagina,
                                    @RequestParam(defaultValue = "20") @Min(1) @Max(100) int tamano) {
        return servicio.listar(filaId, k, pagina, tamano);
    }

    @PostMapping("/{filaId}/siguiente")
    @Operation(summary = "Entrega el siguiente turno según la regla K; 204 si la fila está vacía")
    public ResponseEntity<SiguienteResponse> siguiente(@PathVariable @Pattern(regexp = PATRON_FILA) String filaId,
                                                       @Valid @RequestBody SiguienteRequest peticion) {
        return servicio.siguiente(filaId, peticion.maxPreferencialesSeguidos())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/{filaId}/turnos/{turnoId}/posicion")
    @Operation(summary = "Personas delante de un turno; 404 si no está en la fila")
    public ResponseEntity<PosicionResponse> posicion(@PathVariable @Pattern(regexp = PATRON_FILA) String filaId,
                                                     @PathVariable UUID turnoId,
                                                     @RequestParam(defaultValue = "2") @Min(1) @Max(10) int k) {
        return servicio.posicion(filaId, turnoId.toString(), k)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{filaId}/turnos/{turnoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Retira un turno cancelado o ausente; idempotente")
    public void retirar(@PathVariable @Pattern(regexp = PATRON_FILA) String filaId, @PathVariable UUID turnoId) {
        servicio.retirar(filaId, turnoId.toString());
    }

    @GetMapping("/{filaId}")
    @Operation(summary = "Tamaño de cada subfila y racha de preferenciales")
    public EstadoFilaResponse estado(@PathVariable @Pattern(regexp = PATRON_FILA) String filaId) {
        return servicio.estado(filaId);
    }
}
