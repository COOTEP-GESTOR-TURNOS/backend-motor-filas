-- Foto atómica para calcular las personas delante de un turno.
-- KEYS[1] = fila:{id}:pref   KEYS[2] = fila:{id}:gen   ARGV[1] = turnoId
-- Devuelve { tipo, rango en su subfila, total preferenciales, total generales } o una lista vacía.

local rango = redis.call('ZRANK', KEYS[1], ARGV[1])
if rango then
  return { 'PREFERENCIAL', rango, redis.call('ZCARD', KEYS[1]), redis.call('ZCARD', KEYS[2]) }
end
rango = redis.call('ZRANK', KEYS[2], ARGV[1])
if rango then
  return { 'GENERAL', rango, redis.call('ZCARD', KEYS[1]), redis.call('ZCARD', KEYS[2]) }
end
return {}
