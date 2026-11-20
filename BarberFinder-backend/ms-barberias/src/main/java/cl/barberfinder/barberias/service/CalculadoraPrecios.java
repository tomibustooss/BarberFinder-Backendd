package cl.barberfinder.barberias.service;

import cl.barberfinder.barberias.model.TipoServicio;

import java.util.List;

/**
 * Misma regla de precios que PricingCalculator en la app:
 * corte clásico y moderno usan su precio base; "Corte y barba" suma
 * corte clásico + barba + adicional de combo, aplicado una sola vez.
 */
public final class CalculadoraPrecios {

    public static final int ADICIONAL_COMBO = 1_500;
    public static final int PRECIO_MINIMO = 1_000;
    public static final int PRECIO_MAXIMO = 100_000;

    public record LineaPrecio(String etiqueta, int monto) {
    }

    public record Desglose(List<LineaPrecio> lineas) {
        public int total() {
            return lineas.stream().mapToInt(LineaPrecio::monto).sum();
        }
    }

    private CalculadoraPrecios() {
    }

    public static Desglose desglose(int corteClasico, int corteModerno, int barba, TipoServicio servicio) {
        List<LineaPrecio> lineas = switch (servicio) {
            case CORTE_CLASICO -> List.of(new LineaPrecio("Corte clásico", corteClasico));
            case CORTE_MODERNO -> List.of(new LineaPrecio("Corte moderno", corteModerno));
            case CORTE_Y_BARBA -> List.of(
                    new LineaPrecio("Corte clásico", corteClasico),
                    new LineaPrecio("Arreglo de barba", barba),
                    new LineaPrecio("Adicional por combo", ADICIONAL_COMBO)
            );
        };
        Desglose d = new Desglose(lineas);
        boolean lineasPositivas = lineas.stream().allMatch(l -> l.monto() > 0);
        if (!lineasPositivas || !esValido(d.total())) {
            throw new IllegalArgumentException("Precio inválido para " + servicio.getEtiqueta() + ": " + d.total());
        }
        return d;
    }

    public static boolean esValido(int precio) {
        return precio >= PRECIO_MINIMO && precio <= PRECIO_MAXIMO;
    }
}
