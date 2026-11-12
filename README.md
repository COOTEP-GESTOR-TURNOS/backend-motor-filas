# motor-filas · COOTEP Turno

Motor de filas en **Java 21 y Spring Boot 3.5, arquitectura MVC**. Servicio interno: no publica
puertos y solo lo llama el backend principal con la cabecera `X-Service-Token`.

| Capa | Paquete | Contenido |
| --- | --- | --- |
| Controlador | `controller` | `FilaController`, `AgenciaController`, `ManejadorErrores` (RFC 9457) |
| Servicio | `service` | `FilaService`, `CalculoPosicion` |
| Modelo | `model` | Peticiones y respuestas (`Modelos`) |
| Repositorio | `repository` | `FilaRedisRepository` + scripts Lua en `src/main/resources/scripts` |
| Configuración | `config` | `AsyncConfig` (ejecutor), `ServiceTokenFilter`, `MotorProperties` |

## API

| Método y ruta | Uso |
| --- | --- |
| `POST /api/v1/filas/{filaId}/turnos` | Encola; idempotente por `turnoId` |
| `POST /api/v1/filas/{filaId}/siguiente` | Entrega el siguiente con la regla K; 204 si está vacía |
| `GET /api/v1/filas/{filaId}/turnos/{turnoId}/posicion?k=2` | Personas delante |
| `DELETE /api/v1/filas/{filaId}/turnos/{turnoId}` | Retira; idempotente |
| `GET /api/v1/filas/{filaId}` | Tamaño de cada subfila |
| `POST /api/v1/agencias/{agenciaId}/cierre` | Vacía en paralelo las filas de la agencia |

`filaId` = `agenciaId:servicioId`. Contrato OpenAPI en `/v3/api-docs`.

## Concurrencia (sección 6 del informe)

- **Escrituras compartidas:** cada operación es un script Lua; Redis lo ejecuta completo sin
  intercalar comandos, así que 200 solicitudes simultáneas reciben números únicos y dos
  funcionarios nunca reciben el mismo turno.
- **Ejecutor del framework:** `AsyncConfig` define `cierreExecutor` (4 a 8 hilos, cola de 100,
  espera 20 s al apagar). `FilaService.cerrarAgencia` vacía cada fila de la agencia en paralelo.

## Ejecutar y probar

```bash
mvn spring-boot:run      # con las variables de .env.example
mvn test                 # las pruebas de concurrencia levantan Redis con Testcontainers (requiere Docker)
```

Sin Maven instalado, use la imagen oficial:

```bash
docker run --rm -v "$PWD":/app -w /app maven:3.9.9-eclipse-temurin-21 mvn test
```
