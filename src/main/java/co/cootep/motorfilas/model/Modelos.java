package co.cootep.motorfilas.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Modelos (la M de MVC): cuerpos de petición y respuesta del motor de filas. */
public final class Modelos {

    private Modelos() {
    }

    public enum TipoAtencion { GENERAL, PREFERENCIAL }

    public record EncolarRequest(
            @NotNull UUID turnoId,
            @NotNull TipoAtencion tipoAtencion,
            @Min(1) @Max(10) int maxPreferencialesSeguidos) {
    }

    public record EncolarResponse(long numero, TipoAtencion tipoAtencion, long personasDelante) {
    }

    public record SiguienteRequest(@Min(1) @Max(10) int maxPreferencialesSeguidos) {
    }

    public record SiguienteResponse(String turnoId, long numero, TipoAtencion tipoAtencion) {
    }

    public record PosicionResponse(TipoAtencion tipoAtencion, long personasDelante) {
    }

    public record EstadoFilaResponse(long preferenciales, long generales, long rachaPreferencial) {
    }

    public record PaginaFilaResponse(
            List<SiguienteResponse> elementos, long total, int pagina, int tamano, long totalPaginas) {
    }

    public record CierreRequest(@NotEmpty List<@NotNull UUID> servicioIds) {
    }

    public record CierreResponse(Map<String, List<String>> retiradosPorServicio, long duracionMs) {
    }

    /** Foto atómica de una fila para calcular las personas delante. */
    public record Instantanea(TipoAtencion tipo, long rango, long preferenciales, long generales) {
    }
}
