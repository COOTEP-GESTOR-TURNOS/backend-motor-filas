package co.cootep.motorfilas;

import static org.assertj.core.api.Assertions.assertThat;

import co.cootep.motorfilas.service.FilaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/** El contexto arranca sin Redis (la conexión es perezosa) y el ejecutor queda con sus límites. */
@SpringBootTest(properties = "motor.service-token=token-de-prueba")
class MotorFilasApplicationTest {

    @Autowired
    private FilaService servicio;

    @Autowired
    @Qualifier("cierreExecutor")
    private ThreadPoolTaskExecutor cierreExecutor;

    @Test
    void cargaElContexto() {
        assertThat(servicio).isNotNull();
        assertThat(cierreExecutor.getCorePoolSize()).isEqualTo(4);
        assertThat(cierreExecutor.getMaxPoolSize()).isEqualTo(8);
    }
}
