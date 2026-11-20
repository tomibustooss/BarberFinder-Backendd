package cl.barberfinder.reservas.model;

public enum EstadoReserva {
    CONFIRMADA("Confirmada"),
    CANCELADA("Cancelada"),
    ATENDIDA("Atendida"),
    NO_ASISTIO("No asistió");

    private final String etiqueta;

    EstadoReserva(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
