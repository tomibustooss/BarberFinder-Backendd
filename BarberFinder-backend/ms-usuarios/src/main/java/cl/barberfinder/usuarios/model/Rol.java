package cl.barberfinder.usuarios.model;

/**
 * Roles de BarberFinder. Cada uno tiene privilegios distintos:
 * ADMIN administra usuarios y todo el sistema.
 * DUENO crea y edita sus barberías y ve sus reservas.
 * BARBERO ve la agenda de su barbería y marca reservas como atendidas.
 * CLIENTE reserva, reprograma y cancela sus propias horas.
 */
public enum Rol {
    ADMIN("Administrador"),
    DUENO("Dueño de barbería"),
    BARBERO("Barbero"),
    CLIENTE("Cliente");

    private final String descripcion;

    Rol(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
