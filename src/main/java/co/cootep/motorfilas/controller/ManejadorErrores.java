package co.cootep.motorfilas.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Errores en formato Problem Details (RFC 9457). La clase base ya traduce las validaciones a 400;
 * aquí se agregan los casos propios del motor.
 */
@RestControllerAdvice
public class ManejadorErrores extends ResponseEntityExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ManejadorErrores.class);

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail datosInvalidos(IllegalArgumentException error) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, error.getMessage());
        problema.setTitle("Datos inválidos");
        problema.setProperty("codigo", "DATOS_INVALIDOS");
        return problema;
    }

    @ExceptionHandler(RedisConnectionFailureException.class)
    public ProblemDetail redisNoDisponible(RedisConnectionFailureException error) {
        LOG.warn("Redis no disponible: {}", error.getMessage());
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,
                "El almacén de filas no está disponible");
        problema.setTitle("Servicio no disponible");
        problema.setProperty("codigo", "REDIS_NO_DISPONIBLE");
        return problema;
    }
}
