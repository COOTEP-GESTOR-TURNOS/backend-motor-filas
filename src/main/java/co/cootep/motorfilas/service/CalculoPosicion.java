package co.cootep.motorfilas.service;

import co.cootep.motorfilas.model.Modelos.TipoAtencion;

/**
 * Personas delante de un turno según la regla K (sección 3 del informe):
 * fila general: g + min(P, K·(g+1)); fila preferencial: r + min(G, ⌊r/K⌋).
 * g y r son el rango del turno en su subfila; P y G, el tamaño de la otra subfila.
 */
public final class CalculoPosicion {

    private CalculoPosicion() {
    }

    public static long personasDelante(TipoAtencion tipo, long rango, long preferenciales, long generales, int k) {
        if (k < 1) {
            throw new IllegalArgumentException("K debe ser al menos 1");
        }
        if (tipo == TipoAtencion.PREFERENCIAL) {
            return rango + Math.min(generales, rango / k);
        }
        return rango + Math.min(preferenciales, (long) k * (rango + 1));
    }
}
