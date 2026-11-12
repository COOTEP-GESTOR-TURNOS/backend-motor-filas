package co.cootep.motorfilas.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.cootep.motorfilas.model.Modelos.TipoAtencion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** PR-09: cálculo de personas delante con la regla K. */
class CalculoPosicionTest {

    @Test
    @DisplayName("El primero de la fila general sin preferenciales no tiene a nadie delante")
    void primeroSinPreferenciales() {
        assertThat(CalculoPosicion.personasDelante(TipoAtencion.GENERAL, 0, 0, 3, 2)).isZero();
    }

    @Test
    @DisplayName("Delante del primero general pasan como máximo K preferenciales")
    void primeroGeneralConPreferenciales() {
        assertThat(CalculoPosicion.personasDelante(TipoAtencion.GENERAL, 0, 5, 1, 2)).isEqualTo(2);
    }

    @Test
    @DisplayName("Si hay pocos preferenciales, solo cuentan los que existen")
    void generalConPocosPreferenciales() {
        assertThat(CalculoPosicion.personasDelante(TipoAtencion.GENERAL, 2, 1, 5, 2)).isEqualTo(3);
    }

    @Test
    @DisplayName("Un preferencial cede un general cada K preferenciales")
    void preferencialCedeGenerales() {
        assertThat(CalculoPosicion.personasDelante(TipoAtencion.PREFERENCIAL, 0, 4, 10, 2)).isZero();
        assertThat(CalculoPosicion.personasDelante(TipoAtencion.PREFERENCIAL, 3, 4, 10, 2)).isEqualTo(4);
    }

    @Test
    @DisplayName("K menor que 1 no es válido")
    void kInvalido() {
        assertThatThrownBy(() -> CalculoPosicion.personasDelante(TipoAtencion.GENERAL, 0, 0, 0, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
