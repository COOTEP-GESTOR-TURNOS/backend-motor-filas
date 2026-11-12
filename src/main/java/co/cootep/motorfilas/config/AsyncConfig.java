package co.cootep.motorfilas.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Ejecutor administrado por Spring (sección 6 del informe). Aquí se crean y limitan las tareas
 * del cierre de agencia; al apagar, Spring espera hasta 20 s a que terminen y luego cierra el pool.
 */
@Configuration
public class AsyncConfig {

    @Bean(name = "cierreExecutor")
    public ThreadPoolTaskExecutor cierreExecutor(MotorProperties propiedades) {
        ThreadPoolTaskExecutor ejecutor = new ThreadPoolTaskExecutor();
        ejecutor.setCorePoolSize(propiedades.cierre().hilosBase());
        ejecutor.setMaxPoolSize(propiedades.cierre().hilosMax());
        ejecutor.setQueueCapacity(propiedades.cierre().capacidadCola());
        ejecutor.setThreadNamePrefix("cierre-filas-");
        ejecutor.setWaitForTasksToCompleteOnShutdown(true);
        ejecutor.setAwaitTerminationSeconds(20);
        return ejecutor;
    }
}
