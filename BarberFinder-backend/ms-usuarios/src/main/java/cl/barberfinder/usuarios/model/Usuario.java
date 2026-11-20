package cl.barberfinder.usuarios.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 60)
    private String nombre;

    @Column(nullable = false, unique = true, length = 120)
    private String email;

    @Column(length = 15)
    private String telefono;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Rol rol;

    @Column(name = "barberia_id")
    private Long barberiaId;

    @Column(nullable = false)
    private boolean activo = true;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn;

    @Column(name = "codigo_recuperacion_hash", length = 100)
    private String codigoRecuperacionHash;

    @Column(name = "codigo_expira_en")
    private LocalDateTime codigoExpiraEn;

    @Column(name = "intentos_recuperacion", nullable = false)
    private int intentosRecuperacion = 0;

    @PrePersist
    void alCrear() {
        if (creadoEn == null) {
            creadoEn = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public Rol getRol() { return rol; }
    public void setRol(Rol rol) { this.rol = rol; }

    public Long getBarberiaId() { return barberiaId; }
    public void setBarberiaId(Long barberiaId) { this.barberiaId = barberiaId; }

    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }

    public LocalDateTime getCreadoEn() { return creadoEn; }
    public void setCreadoEn(LocalDateTime creadoEn) { this.creadoEn = creadoEn; }

    public String getCodigoRecuperacionHash() { return codigoRecuperacionHash; }
    public void setCodigoRecuperacionHash(String codigoRecuperacionHash) { this.codigoRecuperacionHash = codigoRecuperacionHash; }

    public LocalDateTime getCodigoExpiraEn() { return codigoExpiraEn; }
    public void setCodigoExpiraEn(LocalDateTime codigoExpiraEn) { this.codigoExpiraEn = codigoExpiraEn; }

    public int getIntentosRecuperacion() { return intentosRecuperacion; }
    public void setIntentosRecuperacion(int intentosRecuperacion) { this.intentosRecuperacion = intentosRecuperacion; }
}
