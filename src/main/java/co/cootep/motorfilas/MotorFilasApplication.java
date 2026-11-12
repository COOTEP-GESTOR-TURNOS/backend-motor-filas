package co.cootep.motorfilas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Motor de filas de COOTEP Turno. Servicio interno (sin puerto publicado) que mantiene el orden
 * de cada fila en Redis con operaciones atómicas. Lo llama solo el backend principal.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class MotorFilasApplication {

    public static void main(String[] args) {
        SpringApplication.run(MotorFilasApplication.class, args);
    }
}
