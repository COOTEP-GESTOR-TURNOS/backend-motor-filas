-- Entrega el siguiente turno de forma atómica (RN-08 y RN-09).
-- KEYS[1] = fila:{id}:pref   KEYS[2] = fila:{id}:gen   KEYS[3] = fila:{id}:racha
-- ARGV[1] = K (preferenciales seguidos permitidos)   ARGV[2] = TTL en segundos
-- Toma primero la fila preferencial; tras K preferenciales seguidos atiende uno general si hay espera.
-- Redis ejecuta el script completo sin interrupciones: dos funcionarios nunca reciben el mismo turno.

local k = tonumber(ARGV[1])
local racha = tonumber(redis.call('GET', KEYS[3]) or '0')
local hayPreferencial = redis.call('ZCARD', KEYS[1]) > 0
local hayGeneral = redis.call('ZCARD', KEYS[2]) > 0

local origen, tipo
if hayPreferencial and (racha < k or not hayGeneral) then
  origen, tipo = KEYS[1], 'PREFERENCIAL'
  racha = racha + 1
elseif hayGeneral then
  origen, tipo = KEYS[2], 'GENERAL'
  racha = 0
else
  return {} -- fila vacía: lista vacía (un nil llega a Java como [null])
end

local entregado = redis.call('ZPOPMIN', origen)
redis.call('SET', KEYS[3], racha, 'EX', tonumber(ARGV[2]))
return { entregado[1], entregado[2], tipo }
