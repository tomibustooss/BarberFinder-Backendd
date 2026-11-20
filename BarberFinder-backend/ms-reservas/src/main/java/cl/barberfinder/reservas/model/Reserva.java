package cl.barberfinder.reservas.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "reservas", indexes = {
        @Index(name = "idx_reserva_horario", columnList = "barberia_id, fecha, hora"),
        @Index(name = "idx_reserva_cliente", columnList = "cliente_id")
})
public class Reserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cliente_id", nullable = false)
    private Long clienteId;

    @Column(name = "barberia_id", nullable = false)
    private Long barberiaId;

    @Column(name = "barberia_nombre", nullable = false, length = 80)
    private String barberiaNombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoServicio servicio;

    @Column(nullable = false)
    private Integer precio;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(nullable = false, length = 5)
    private String hora;

    @Column(name = "nombre_cliente", nullable = false, length = 60)
    private String nombreCliente;

    @Column(name = "telefono_cliente", nullable = false, length = 15)
    private String telefonoCliente;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoReserva estado = EstadoReserva.CONFIRMADA;

    @Column(name = "creada_en", nullable = false)
    private LocalDateTime creadaEn;

    @Column(name = "actualizada_en")
    private LocalDateTime actualizadaEn;

    @PrePersist
    void alCrear() {
        if (creadaEn == null) {
            creadaEn = LocalDateTime.now();
        }
    }

    @PreUpdate
    void alActualizar() {
        actualizadaEn = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getClienteId() { return clienteId; }
    public void setClienteId(Long clienteId) { this.clienteId = clienteId; }

    public Long getBarberiaId() { return barberiaId; }
    public void setBarberiaId(Long barberiaId) { this.barberiaId = barberiaId; }

    public String getBarberiaNombre() { return barberiaNombre; }
    public void setBarberiaNombre(String barberiaNombre) { this.barberiaNombre = barberiaNombre; }

    public TipoServicio getServicio() { return servicio; }
    public void setServicio(TipoServicio servicio) { this.servicio = servicio; }

    public Integer getPrecio() { return precio; }
    public void setPrecio(Integer precio) { this.precio = precio; }

    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }

    public String getHora() { return hora; }
    public void setHora(String hora) { this.hora = hora; }

    public String getNombreCliente() { return nombreCliente; }
    public void setNombreCliente(String nombreCliente) { this.nombreCliente = nombreCliente; }

    public String getTelefonoCliente() { return telefonoCliente; }
    public void setTelefonoCliente(String telefonoCliente) { this.telefonoCliente = telefonoCliente; }

    public EstadoReserva getEstado() { return estado; }
    public void setEstado(EstadoReserva estado) { this.estado = estado; }

    public LocalDateTime getCreadaEn() { return creadaEn; }
    public void setCreadaEn(LocalDateTime creadaEn) { this.creadaEn = creadaEn; }

    public LocalDateTime getActualizadaEn() { return actualizadaEn; }
    public void setActualizadaEn(LocalDateTime actualizadaEn) { this.actualizadaEn = actualizadaEn; }
}
