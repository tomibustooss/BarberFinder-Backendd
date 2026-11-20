package cl.barberfinder.barberias;

import cl.barberfinder.barberias.model.TipoServicio;
import cl.barberfinder.barberias.service.CalculadoraPrecios;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CalculadoraPreciosTest {

    @Test
    void corteClasicoUsaSuPrecioBase() {
        assertEquals(6_000, CalculadoraPrecios.desglose(6_000, 7_000, 3_000, TipoServicio.CORTE_CLASICO).total());
    }

    @Test
    void corteModernoUsaSuPrecioBase() {
        assertEquals(7_000, CalculadoraPrecios.desglose(6_000, 7_000, 3_000, TipoServicio.CORTE_MODERNO).total());
    }

    @Test
    void comboSumaAdicionalUnaSolaVez() {
        CalculadoraPrecios.Desglose d = CalculadoraPrecios.desglose(6_000, 7_000, 3_000, TipoServicio.CORTE_Y_BARBA);
        assertEquals(3, d.lineas().size());
        assertEquals(10_500, d.total());
    }

    @Test
    void rechazaPreciosFueraDeRango() {
        assertThrows(IllegalArgumentException.class,
                () -> CalculadoraPrecios.desglose(500, 7_000, 3_000, TipoServicio.CORTE_CLASICO));
        assertThrows(IllegalArgumentException.class,
                () -> CalculadoraPrecios.desglose(60_000, 7_000, 50_000, TipoServicio.CORTE_Y_BARBA));
    }

    @Test
    void limitesDeRango() {
        assertTrue(CalculadoraPrecios.esValido(1_000));
        assertTrue(CalculadoraPrecios.esValido(100_000));
        assertFalse(CalculadoraPrecios.esValido(999));
        assertFalse(CalculadoraPrecios.esValido(100_001));
    }
}
