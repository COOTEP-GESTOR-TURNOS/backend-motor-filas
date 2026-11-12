-- Retira un turno de las dos subfilas (cancelado o ausente, RN-12). Idempotente.
-- KEYS[1] = fila:{id}:pref   KEYS[2] = fila:{id}:gen   ARGV[1] = turnoId
return redis.call('ZREM', KEYS[1], ARGV[1]) + redis.call('ZREM', KEYS[2], ARGV[1])
