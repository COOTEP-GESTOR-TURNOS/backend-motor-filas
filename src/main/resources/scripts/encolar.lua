-- Encola un turno de forma atómica e idempotente (RN-02).
-- KEYS[1] = fila:{id}:pref   KEYS[2] = fila:{id}:gen
-- KEYS[3] = fila:{id}:seq:{fecha}   KEYS[4] = fila:{id}:numeros (hash turnoId -> número)
-- ARGV[1] = turnoId   ARGV[2] = GENERAL | PREFERENCIAL   ARGV[3] = TTL en segundos
-- Si el turno ya tenía número, conserva el original: un reintento no duplica y un preferencial
-- no verificado vuelve a la fila general en su puesto de llegada (RN-07).

local turno = ARGV[1]
local destino, otra = KEYS[2], KEYS[1]
if ARGV[2] == 'PREFERENCIAL' then
  destino, otra = KEYS[1], KEYS[2]
end

local numero = redis.call('HGET', KEYS[4], turno)
if numero then
  numero = tonumber(numero)
else
  numero = redis.call('INCR', KEYS[3])
  redis.call('EXPIRE', KEYS[3], 129600) -- el contador del día vive 36 h
  redis.call('HSET', KEYS[4], turno, numero)
end

redis.call('ZREM', otra, turno)
redis.call('ZADD', destino, numero, turno)

local ttl = tonumber(ARGV[3])
redis.call('EXPIRE', KEYS[1], ttl)
redis.call('EXPIRE', KEYS[2], ttl)
redis.call('EXPIRE', KEYS[4], ttl)
return numero
