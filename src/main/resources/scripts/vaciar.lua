-- Vacía una fila al cierre de la agencia (RN-13) y devuelve los turnos retirados.
-- KEYS[1] = fila:{id}:pref   KEYS[2] = fila:{id}:gen   KEYS[3] = fila:{id}:racha

local retirados = redis.call('ZRANGE', KEYS[1], 0, -1)
for _, turno in ipairs(redis.call('ZRANGE', KEYS[2], 0, -1)) do
  table.insert(retirados, turno)
end
redis.call('DEL', KEYS[1], KEYS[2], KEYS[3])
return retirados
