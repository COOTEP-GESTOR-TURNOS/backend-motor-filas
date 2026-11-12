package co.cootep.motorfilas.repository;

import co.cootep.motorfilas.model.Modelos.Instantanea;
import co.cootep.motorfilas.model.Modelos.SiguienteResponse;
import co.cootep.motorfilas.model.Modelos.TipoAtencion;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Repository;

/**
 * Acceso a Redis. Cada operación que escribe es un script Lua: Redis lo ejecuta completo sin
 * intercalar otros comandos, así que no hay carreras entre peticiones simultáneas.
 * Claves: fila:{agencia}:{servicio}:pref | :gen | :seq:{fecha} | :numeros | :racha
 */
@Repository
@SuppressWarnings({"rawtypes", "unchecked"})
public class FilaRedisRepository {

    private static final ZoneId ZONA_AGENCIAS = ZoneId.of("America/Bogota");
    private static final RedisScript<Long> ENCOLAR = script("encolar", Long.class);
    private static final RedisScript<List> SIGUIENTE = script("siguiente", List.class);
    private static final RedisScript<List> POSICION = script("posicion", List.class);
    private static final RedisScript<Long> RETIRAR = script("retirar", Long.class);
    private static final RedisScript<List> VACIAR = script("vaciar", List.class);

    private final StringRedisTemplate redis;

    public FilaRedisRepository(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /** Devuelve el número del turno; si ya estaba encolado, el mismo número (idempotente). */
    public long encolar(String filaId, String turnoId, TipoAtencion tipo, long ttlSegundos) {
        String secuencia = clave(filaId, "seq:" + LocalDate.now(ZONA_AGENCIAS));
        Long numero = redis.execute(ENCOLAR,
                List.of(clave(filaId, "pref"), clave(filaId, "gen"), secuencia, clave(filaId, "numeros")),
                turnoId, tipo.name(), Long.toString(ttlSegundos));
        return Objects.requireNonNull(numero, "El script de encolar no devolvió número");
    }

    public Optional<SiguienteResponse> siguiente(String filaId, int k, long ttlSegundos) {
        List<Object> r = redis.execute(SIGUIENTE,
                List.of(clave(filaId, "pref"), clave(filaId, "gen"), clave(filaId, "racha")),
                Integer.toString(k), Long.toString(ttlSegundos));
        if (r == null || r.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new SiguienteResponse(
                (String) r.get(0), (long) Double.parseDouble((String) r.get(1)), TipoAtencion.valueOf((String) r.get(2))));
    }

    public Optional<Instantanea> posicion(String filaId, String turnoId) {
        List<Object> r = redis.execute(POSICION, List.of(clave(filaId, "pref"), clave(filaId, "gen")), turnoId);
        if (r == null || r.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new Instantanea(TipoAtencion.valueOf((String) r.get(0)),
                ((Number) r.get(1)).longValue(), ((Number) r.get(2)).longValue(), ((Number) r.get(3)).longValue()));
    }

    public long retirar(String filaId, String turnoId) {
        Long retirados = redis.execute(RETIRAR, List.of(clave(filaId, "pref"), clave(filaId, "gen")), turnoId);
        return retirados == null ? 0 : retirados;
    }

    public List<String> vaciar(String filaId) {
        List<String> retirados = redis.execute(VACIAR,
                List.of(clave(filaId, "pref"), clave(filaId, "gen"), clave(filaId, "racha")));
        return retirados == null ? List.of() : retirados;
    }

    public long tamano(String filaId, String subfila) {
        Long tamano = redis.opsForZSet().zCard(clave(filaId, subfila));
        return tamano == null ? 0 : tamano;
    }

    public long racha(String filaId) {
        String valor = redis.opsForValue().get(clave(filaId, "racha"));
        return valor == null ? 0 : Long.parseLong(valor);
    }

    private static String clave(String filaId, String sufijo) {
        return "fila:" + filaId + ":" + sufijo;
    }

    private static <T> RedisScript<T> script(String nombre, Class<T> tipo) {
        return RedisScript.of(new ClassPathResource("scripts/" + nombre + ".lua"), tipo);
    }
}
