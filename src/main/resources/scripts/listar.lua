-- Página de la fila en el mismo orden de entrega que siguiente.lua.
-- KEYS: preferenciales, generales y racha. ARGV: K, desplazamiento y tamaño.
local k = tonumber(ARGV[1])
local desplazamiento = tonumber(ARGV[2])
local tamano = tonumber(ARGV[3])
local preferenciales = redis.call('ZCARD', KEYS[1])
local generales = redis.call('ZCARD', KEYS[2])
local total = preferenciales + generales
local resultado = { total }
if desplazamiento >= total then
  return resultado
end

local racha = tonumber(redis.call('GET', KEYS[3]) or '0')
local prefIniciales = math.max(0, k - racha)

-- Tras el primer bloque de preferenciales, cada ciclo consume 1 general y K preferenciales.
-- Si se agota una subfila, la otra completa el prefijo.
local function generalesEnPrefijo(cantidad)
  local enCiclos = math.max(0, 1 + math.floor((cantidad - prefIniciales - 1) / (k + 1)))
  return math.min(generales, math.max(cantidad - preferenciales, enCiclos))
end

local fin = math.min(total, desplazamiento + tamano)
local inicioGen, finGen = generalesEnPrefijo(desplazamiento), generalesEnPrefijo(fin)
local inicioPref, finPref = desplazamiento - inicioGen, fin - finGen
local paginaGen, paginaPref = {}, {}
if finGen > inicioGen then
  paginaGen = redis.call('ZRANGE', KEYS[2], inicioGen, finGen - 1, 'WITHSCORES')
end
if finPref > inicioPref then
  paginaPref = redis.call('ZRANGE', KEYS[1], inicioPref, finPref - 1, 'WITHSCORES')
end

local indiceGen, indicePref = 1, 1
local ultimoGen = inicioGen
for posicion = desplazamiento + 1, fin do
  local consumidosGen = generalesEnPrefijo(posicion)
  if consumidosGen > ultimoGen then
    table.insert(resultado, paginaGen[indiceGen])
    table.insert(resultado, paginaGen[indiceGen + 1])
    table.insert(resultado, 'GENERAL')
    indiceGen = indiceGen + 2
  else
    table.insert(resultado, paginaPref[indicePref])
    table.insert(resultado, paginaPref[indicePref + 1])
    table.insert(resultado, 'PREFERENCIAL')
    indicePref = indicePref + 2
  end
  ultimoGen = consumidosGen
end
return resultado
