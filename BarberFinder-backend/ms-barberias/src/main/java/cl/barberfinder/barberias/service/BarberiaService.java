package cl.barberfinder.barberias.service;

import cl.barberfinder.barberias.dto.BarberiaDtos.BarberiaRequest;
import cl.barberfinder.barberias.dto.BarberiaDtos.BarberiaResponse;
import cl.barberfinder.barberias.dto.BarberiaDtos.PrecioResponse;
import cl.barberfinder.barberias.exception.ApiException;
import cl.barberfinder.barberias.model.Barberia;
import cl.barberfinder.barberias.model.Categoria;
import cl.barberfinder.barberias.model.TipoServicio;
import cl.barberfinder.barberias.repository.BarberiaRepository;
import cl.barberfinder.barberias.security.AuthUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@Service
public class BarberiaService {

    private final BarberiaRepository repositorio;

    public BarberiaService(BarberiaRepository repositorio) {
        this.repositorio = repositorio;
    }

    /** Búsqueda que ignora tildes y mayúsculas, igual que en la app. */
    @Transactional(readOnly = true)
    public List<BarberiaResponse> listar(Categoria categoria, String texto) {
        String filtro = normalizar(texto);
        return repositorio.findByActivaTrueOrderByIdAsc().stream()
                .filter(b -> categoria == null || b.getCategoria() == categoria)
                .filter(b -> filtro.isEmpty() || normalizar(b.getNombre()).contains(filtro))
                .map(BarberiaResponse::desde)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BarberiaResponse> listarDeDueno(Long duenoId) {
        return repositorio.findByDuenoIdOrderByIdAsc(duenoId).stream()
                .map(BarberiaResponse::desde)
                .toList();
    }

    @Transactional(readOnly = true)
    public BarberiaResponse obtener(Long id) {
        return BarberiaResponse.desde(buscar(id));
    }

    @Transactional(readOnly = true)
    public PrecioResponse precio(Long id, TipoServicio servicio) {
        Barberia b = buscar(id);
        if (!b.isActiva()) {
            throw ApiException.solicitudInvalida("La barbería no está disponible.");
        }
        try {
            CalculadoraPrecios.Desglose d = CalculadoraPrecios.desglose(
                    b.getPrecioCorteClasico(), b.getPrecioCorteModerno(), b.getPrecioBarba(), servicio);
            return new PrecioResponse(b.getId(), b.getNombre(), b.getDuenoId(), servicio,
                    servicio.getEtiqueta(), servicio.getDescripcion(), d.lineas(), d.total());
        } catch (IllegalArgumentException e) {
            throw ApiException.solicitudInvalida("El precio de este servicio no es válido.");
        }
    }

    @Transactional(readOnly = true)
    public List<PrecioResponse> servicios(Long id) {
        return Arrays.stream(TipoServicio.values())
                .map(tipo -> precio(id, tipo))
                .toList();
    }

    @Transactional
    public BarberiaResponse crear(BarberiaRequest req, AuthUser auth) {
        Long duenoId = auth.esAdmin() && req.duenoId() != null ? req.duenoId() : auth.id();
        Barberia b = new Barberia();
        aplicar(b, req);
        b.setDuenoId(duenoId);
        b.setActiva(true);
        return BarberiaResponse.desde(repositorio.save(b));
    }

    @Transactional
    public BarberiaResponse actualizar(Long id, BarberiaRequest req, AuthUser auth) {
        Barberia b = buscar(id);
        validarPropietario(b, auth);
        aplicar(b, req);
        if (auth.esAdmin() && req.duenoId() != null) {
            b.setDuenoId(req.duenoId());
        }
        return BarberiaResponse.desde(repositorio.save(b));
    }

    @Transactional
    public BarberiaResponse cambiarEstado(Long id, boolean activa, AuthUser auth) {
        Barberia b = buscar(id);
        validarPropietario(b, auth);
        b.setActiva(activa);
        return BarberiaResponse.desde(repositorio.save(b));
    }

    @Transactional
    public void eliminar(Long id, AuthUser auth) {
        Barberia b = buscar(id);
        validarPropietario(b, auth);
        repositorio.delete(b);
    }

    private void aplicar(Barberia b, BarberiaRequest req) {
        int combo = req.precioCorteClasico() + req.precioBarba() + CalculadoraPrecios.ADICIONAL_COMBO;
        if (!CalculadoraPrecios.esValido(combo)) {
            throw ApiException.solicitudInvalida("El combo corte y barba ($" + combo + ") supera el máximo de $100.000.");
        }
        b.setNombre(req.nombre().trim());
        b.setCategoria(req.categoria());
        b.setDireccion(req.direccion().trim());
        b.setComuna(vacioANulo(req.comuna()));
        b.setDescripcion(vacioANulo(req.descripcion()));
        b.setTelefono(vacioANulo(req.telefono()));
        b.setPrecioCorteClasico(req.precioCorteClasico());
        b.setPrecioCorteModerno(req.precioCorteModerno());
        b.setPrecioBarba(req.precioBarba());
        b.setLatitud(req.latitud());
        b.setLongitud(req.longitud());
    }

    private void validarPropietario(Barberia b, AuthUser auth) {
        if (!auth.esAdmin() && !b.getDuenoId().equals(auth.id())) {
            throw ApiException.prohibido("Solo el dueño de la barbería o un administrador puede modificarla.");
        }
    }

    private Barberia buscar(Long id) {
        return repositorio.findById(id)
                .orElseThrow(() -> ApiException.noEncontrado("Barbería no encontrada."));
    }

    private static String vacioANulo(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    static String normalizar(String s) {
        if (s == null) {
            return "";
        }
        String sinTildes = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sinTildes.toLowerCase(Locale.ROOT).trim();
    }
}
