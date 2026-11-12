package co.cootep.motorfilas.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configuración del motor (prefijo motor en application.yml). */
@ConfigurationProperties(prefix = "motor")
public record MotorProperties(String serviceToken, long ttlFilaSegundos, Cierre cierre) {

    /** Límites del ejecutor que vacía en paralelo las filas de una agencia. */
    public record Cierre(int hilosBase, int hilosMax, int capacidadCola) {
    }
}
