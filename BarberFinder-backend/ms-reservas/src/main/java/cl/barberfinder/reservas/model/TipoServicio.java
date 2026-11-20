package cl.barberfinder.reservas.model;

/** Mismos servicios que ServiceType en la app y TipoServicio en ms-barberias. */
public enum TipoServicio {
    CORTE_CLASICO("Corte clásico"),
    CORTE_MODERNO("Corte moderno"),
    CORTE_Y_BARBA("Corte y barba");

    private final String etiqueta;

    TipoServicio(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
