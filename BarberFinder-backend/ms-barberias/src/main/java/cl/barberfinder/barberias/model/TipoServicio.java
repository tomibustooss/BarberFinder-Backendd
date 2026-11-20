package cl.barberfinder.barberias.model;

/** Mismos servicios que ServiceType en la app. */
public enum TipoServicio {
    CORTE_CLASICO("Corte clásico", "Corte tradicional con tijera y máquina."),
    CORTE_MODERNO("Corte moderno", "Degradados, texturas y estilos actuales."),
    CORTE_Y_BARBA("Corte y barba", "Corte clásico más arreglo de barba.");

    private final String etiqueta;
    private final String descripcion;

    TipoServicio(String etiqueta, String descripcion) {
        this.etiqueta = etiqueta;
        this.descripcion = descripcion;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
