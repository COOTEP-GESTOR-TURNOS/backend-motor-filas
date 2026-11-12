package co.cootep.motorfilas.service;

import co.cootep.motorfilas.config.MotorProperties;
import co.cootep.motorfilas.model.Modelos.CierreResponse;
import co.cootep.motorfilas.model.Modelos.EncolarRequest;
import co.cootep.motorfilas.model.Modelos.EncolarResponse;
import co.cootep.motorfilas.model.Modelos.EstadoFilaResponse;
import co.cootep.motorfilas.model.Modelos.PosicionResponse;
import co.cootep.motorfilas.model.Modelos.SiguienteResponse;
import co.cootep.motorfilas.repository.FilaRedisRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

/** Lógica del motor de filas (la C de MVC delega aquí; el repositorio habla con Redis). */
@Service
public class FilaService {

    private final FilaRedisRepository repositorio;
    private final ThreadPoolTaskExecutor cierreExecutor;
    private final MotorProperties propiedades;

    public FilaService(FilaRedisRepository repositorio,
                       @Qualifier("cierreExecutor") ThreadPoolTaskExecutor cierreExecutor,
                       MotorProperties propiedades) {
        this.repositorio = repositorio;
        this.cierreExecutor = cierreExecutor;
        this.propiedades = propiedades;
    }

    public EncolarResponse encolar(String filaId, EncolarRequest peticion) {
        String turnoId = peticion.turnoId().toString();
        long numero = repositorio.encolar(filaId, turnoId, peticion.tipoAtencion(), propiedades.ttlFilaSegundos());
        long delante = posicion(filaId, turnoId, peticion.maxPreferencialesSeguidos())
                .map(PosicionResponse::personasDelante)
                .orElse(0L);
        return new EncolarResponse(numero, peticion.tipoAtencion(), delante);
    }

    public Optional<SiguienteResponse> siguiente(String filaId, int k) {
        return repositorio.siguiente(filaId, k, propiedades.ttlFilaSegundos());
    }

    public Optional<PosicionResponse> posicion(String filaId, String turnoId, int k) {
        return repositorio.posicion(filaId, turnoId).map(foto -> new PosicionResponse(foto.tipo(),
                CalculoPosicion.personasDelante(foto.tipo(), foto.rango(), foto.preferenciales(), foto.generales(), k)));
    }

    public void retirar(String filaId, String turnoId) {
        repositorio.retirar(filaId, turnoId);
    }

    public EstadoFilaResponse estado(String filaId) {
        return new EstadoFilaResponse(repositorio.tamano(filaId, "pref"), repositorio.tamano(filaId, "gen"),
                repositorio.racha(filaId));
    }

    /**
     * RN-13: vacía en paralelo las filas de todos los servicios de una agencia. Cada fila es una
     * tarea del ejecutor configurado en AsyncConfig; join() espera a que terminen todas.
     */
    public CierreResponse cerrarAgencia(String agenciaId, List<UUID> servicioIds) {
        long inicio = System.nanoTime();
        List<CompletableFuture<Map.Entry<String, List<String>>>> tareas = servicioIds.stream()
                .map(UUID::toString)
                .map(servicioId -> CompletableFuture.supplyAsync(
                        () -> Map.entry(servicioId, repositorio.vaciar(agenciaId + ":" + servicioId)), cierreExecutor))
                .toList();
        Map<String, List<String>> retirados = tareas.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        return new CierreResponse(retirados, (System.nanoTime() - inicio) / 1_000_000);
    }
}
