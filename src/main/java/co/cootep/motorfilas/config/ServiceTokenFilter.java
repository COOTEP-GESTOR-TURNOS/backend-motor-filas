package co.cootep.motorfilas.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Autenticación entre servicios: exige la cabecera X-Service-Token (comparación en tiempo constante)
 * y deja el X-Request-Id del backend principal en los logs. Si no hay token configurado, rechaza todo.
 */
@Component
public class ServiceTokenFilter extends OncePerRequestFilter {

    private final byte[] tokenEsperado;

    public ServiceTokenFilter(MotorProperties propiedades) {
        String token = propiedades.serviceToken() == null ? "" : propiedades.serviceToken();
        this.tokenEsperado = token.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest peticion) {
        String ruta = peticion.getRequestURI();
        return ruta.startsWith("/actuator/health") || ruta.startsWith("/v3/api-docs") || ruta.startsWith("/swagger-ui");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest peticion, HttpServletResponse respuesta, FilterChain cadena)
            throws ServletException, IOException {
        String recibido = peticion.getHeader("X-Service-Token");
        boolean valido = tokenEsperado.length > 0
                && recibido != null
                && MessageDigest.isEqual(tokenEsperado, recibido.getBytes(StandardCharsets.UTF_8));
        if (!valido) {
            respuesta.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            respuesta.setContentType("application/problem+json");
            respuesta.getWriter().write(
                    "{\"type\":\"about:blank\",\"title\":\"Token de servicio inválido\",\"status\":401}");
            return;
        }
        String requestId = peticion.getHeader("X-Request-Id");
        MDC.put("requestId", requestId != null ? requestId : UUID.randomUUID().toString());
        try {
            cadena.doFilter(peticion, respuesta);
        } finally {
            MDC.remove("requestId");
        }
    }
}
