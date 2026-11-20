package co.cootep.motorfilas.service;

import static org.assertj.core.api.Assertions.assertThat;

import co.cootep.motorfilas.model.Modelos.EncolarRequest;
import co.cootep.motorfilas.model.Modelos.PaginaFilaResponse;
import co.cootep.motorfilas.model.Modelos.SiguienteResponse;
import co.cootep.motorfilas.model.Modelos.TipoAtencion;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * PR-06, PR-07 y PR-08: concurrencia real contra un Redis en contenedor.
 * Requiere Docker; sin Docker, JUnit omite la clase (no falla).
 */
@SpringBootTest(properties = "motor.service-token=token-de-prueba")
@Testcontainers(disabledWithoutDocker = true)
class FilaConcurrenciaTest {

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7.4.1-alpine").withExposedPorts(6379);

    @DynamicPropertySource
    static void redis(DynamicPropertyRegistry registro) {
        registro.add("spring.data.redis.host", REDIS::getHost);
        registro.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    }

    @Autowired
    private FilaService servicio;

    private static String nuevaFila() {
        return UUID.randomUUID() + ":" + UUID.randomUUID();
    }

    private EncolarRequest turno(TipoAtencion tipo) {
        return new EncolarRequest(UUID.randomUUID(), tipo, 2);
    }

    @Test
    @DisplayName("PR-06: 200 encolamientos simultáneos reciben números únicos del 1 al 200")
    void doscientosEncolamientosSimultaneos() throws Exception {
        String fila = nuevaFila();
        ExecutorService hilos = Executors.newFixedThreadPool(32);
        CountDownLatch salida = new CountDownLatch(1);
        List<Callable<Long>> tareas = IntStream.range(0, 200)
                .mapToObj(i -> (Callable<Long>) () -> {
                    salida.await(); // todos arrancan a la vez
                    return servicio.encolar(fila, turno(i % 5 == 0 ? TipoAtencion.PREFERENCIAL : TipoAtencion.GENERAL)).numero();
                })
                .toList();
        List<Future<Long>> futuros = new ArrayList<>();
        tareas.forEach(t -> futuros.add(hilos.submit(t)));
        long inicio = System.nanoTime();
        salida.countDown();
        List<Long> numeros = new ArrayList<>();
        for (Future<Long> f : futuros) {
            numeros.add(f.get());
        }
        long ms = (System.nanoTime() - inicio) / 1_000_000;
        hilos.shutdown();

        System.out.printf("200 encolamientos concurrentes en %d ms%n", ms);
        assertThat(new HashSet<>(numeros)).hasSize(200);
        assertThat(Collections.min(numeros)).isEqualTo(1L);
        assertThat(Collections.max(numeros)).isEqualTo(200L);
    }

    @Test
    @DisplayName("PR-07: dos funcionarios llamando a la vez nunca reciben el mismo turno")
    void dosFuncionariosNoRepiten() throws Exception {
        String fila = nuevaFila();
        for (int i = 0; i < 100; i++) {
            servicio.encolar(fila, turno(i % 4 == 0 ? TipoAtencion.PREFERENCIAL : TipoAtencion.GENERAL));
        }
        ExecutorService funcionarios = Executors.newFixedThreadPool(2);
        Callable<List<String>> llamar = () -> {
            List<String> recibidos = new ArrayList<>();
            for (int i = 0; i < 60; i++) {
                servicio.siguiente(fila, 2).map(SiguienteResponse::turnoId).ifPresent(recibidos::add);
            }
            return recibidos;
        };
        Future<List<String>> a = funcionarios.submit(llamar);
        Future<List<String>> b = funcionarios.submit(llamar);
        List<String> todos = new ArrayList<>(a.get());
        todos.addAll(b.get());
        funcionarios.shutdown();

        assertThat(todos).hasSize(100).doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("PR-08: con K = 2 se atienden dos preferenciales y luego un general")
    void respetaLaReglaK() {
        String fila = nuevaFila();
        EncolarRequest g1 = turno(TipoAtencion.GENERAL);
        EncolarRequest p1 = turno(TipoAtencion.PREFERENCIAL);
        EncolarRequest p2 = turno(TipoAtencion.PREFERENCIAL);
        EncolarRequest g2 = turno(TipoAtencion.GENERAL);
        EncolarRequest p3 = turno(TipoAtencion.PREFERENCIAL);
        List.of(g1, p1, p2, g2, p3).forEach(t -> servicio.encolar(fila, t));

        List<String> orden = new ArrayList<>();
        servicio.siguiente(fila, 2).ifPresent(s -> orden.add(s.turnoId()));
        servicio.siguiente(fila, 2).ifPresent(s -> orden.add(s.turnoId()));
        servicio.siguiente(fila, 2).ifPresent(s -> orden.add(s.turnoId()));
        servicio.siguiente(fila, 2).ifPresent(s -> orden.add(s.turnoId()));
        servicio.siguiente(fila, 2).ifPresent(s -> orden.add(s.turnoId()));

        assertThat(orden).containsExactly(
                p1.turnoId().toString(), p2.turnoId().toString(), g1.turnoId().toString(),
                p3.turnoId().toString(), g2.turnoId().toString());
        assertThat(servicio.siguiente(fila, 2)).isEmpty();
    }

    @Test
    @DisplayName("La página sigue la regla K y la racha actual sin consumir turnos")
    void paginaLaFilaSegunElOrdenDeEntrega() {
        String fila = nuevaFila();
        EncolarRequest g1 = turno(TipoAtencion.GENERAL);
        EncolarRequest p1 = turno(TipoAtencion.PREFERENCIAL);
        EncolarRequest p2 = turno(TipoAtencion.PREFERENCIAL);
        EncolarRequest g2 = turno(TipoAtencion.GENERAL);
        EncolarRequest p3 = turno(TipoAtencion.PREFERENCIAL);
        EncolarRequest p4 = turno(TipoAtencion.PREFERENCIAL);
        EncolarRequest g3 = turno(TipoAtencion.GENERAL);
        EncolarRequest p5 = turno(TipoAtencion.PREFERENCIAL);
        List.of(g1, p1, p2, g2, p3, p4, g3, p5).forEach(t -> servicio.encolar(fila, t));
        assertThat(servicio.siguiente(fila, 2).orElseThrow().turnoId()).isEqualTo(p1.turnoId().toString());

        PaginaFilaResponse primera = servicio.listar(fila, 2, 1, 3);
        PaginaFilaResponse segunda = servicio.listar(fila, 2, 2, 3);
        PaginaFilaResponse tercera = servicio.listar(fila, 2, 3, 3);
        assertThat(primera.elementos()).extracting(SiguienteResponse::turnoId)
                .containsExactly(p2.turnoId().toString(), g1.turnoId().toString(), p3.turnoId().toString());
        assertThat(segunda.elementos()).extracting(SiguienteResponse::turnoId)
                .containsExactly(p4.turnoId().toString(), g2.turnoId().toString(), p5.turnoId().toString());
        assertThat(tercera.elementos()).extracting(SiguienteResponse::turnoId).containsExactly(g3.turnoId().toString());
        assertThat(primera.total()).isEqualTo(7);
        assertThat(primera.totalPaginas()).isEqualTo(3);
        assertThat(servicio.listar(fila, 2, 4, 3)).isEqualTo(new PaginaFilaResponse(List.of(), 7, 4, 3, 3));
        assertThat(servicio.listar(nuevaFila(), 2, 1, 3)).isEqualTo(new PaginaFilaResponse(List.of(), 0, 1, 3, 0));

        List<SiguienteResponse> ordenReal = IntStream.range(0, 7)
                .mapToObj(i -> servicio.siguiente(fila, 2).orElseThrow()).toList();
        List<SiguienteResponse> ordenConsultado = new ArrayList<>(primera.elementos());
        ordenConsultado.addAll(segunda.elementos());
        ordenConsultado.addAll(tercera.elementos());
        assertThat(ordenConsultado).containsExactlyElementsOf(ordenReal);
    }

    @Test
    @DisplayName("Encolar dos veces el mismo turno devuelve el mismo número (idempotencia)")
    void encolarEsIdempotente() {
        String fila = nuevaFila();
        EncolarRequest t = turno(TipoAtencion.GENERAL);
        long primero = servicio.encolar(fila, t).numero();
        long segundo = servicio.encolar(fila, t).numero();
        assertThat(segundo).isEqualTo(primero);
        assertThat(servicio.estado(fila).generales()).isEqualTo(1);
    }
}
