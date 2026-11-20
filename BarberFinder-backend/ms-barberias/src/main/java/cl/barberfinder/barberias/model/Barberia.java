package cl.barberfinder.barberias.model;

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
@Table(name = "barberias")
public class Barberia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Categoria categoria;

    @Column(nullable = false, length = 150)
    private String direccion;

    @Column(length = 60)
    private String comuna;

    @Column(length = 300)
    private String descripcion;

    @Column(length = 15)
    private String telefono;

    @Column(name = "precio_corte_clasico", nullable = false)
    private Integer precioCorteClasico;

    @Column(name = "precio_corte_moderno", nullable = false)
    private Integer precioCorteModerno;

    @Column(name = "precio_barba", nullable = false)
    private Integer precioBarba;

    private Double latitud;

    private Double longitud;

    @Column(name = "dueno_id", nullable = false)
    private Long duenoId;

    @Column(nullable = false)
    private boolean activa = true;

    @Column(name = "creada_en", nullable = false)
    private LocalDateTime creadaEn;

    @PrePersist
    void alCrear() {
        if (creadaEn == null) {
            creadaEn = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public Categoria getCategoria() { return categoria; }
    public void setCategoria(Categoria categoria) { this.categoria = categoria; }

    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }

    public String getComuna() { return comuna; }
    public void setComuna(String comuna) { this.comuna = comuna; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public Integer getPrecioCorteClasico() { return precioCorteClasico; }
    public void setPrecioCorteClasico(Integer precioCorteClasico) { this.precioCorteClasico = precioCorteClasico; }

    public Integer getPrecioCorteModerno() { return precioCorteModerno; }
    public void setPrecioCorteModerno(Integer precioCorteModerno) { this.precioCorteModerno = precioCorteModerno; }

    public Integer getPrecioBarba() { return precioBarba; }
    public void setPrecioBarba(Integer precioBarba) { this.precioBarba = precioBarba; }

    public Double getLatitud() { return latitud; }
    public void setLatitud(Double latitud) { this.latitud = latitud; }

    public Double getLongitud() { return longitud; }
    public void setLongitud(Double longitud) { this.longitud = longitud; }

    public Long getDuenoId() { return duenoId; }
    public void setDuenoId(Long duenoId) { this.duenoId = duenoId; }

    public boolean isActiva() { return activa; }
    public void setActiva(boolean activa) { this.activa = activa; }

    public LocalDateTime getCreadaEn() { return creadaEn; }
    public void setCreadaEn(LocalDateTime creadaEn) { this.creadaEn = creadaEn; }
}
