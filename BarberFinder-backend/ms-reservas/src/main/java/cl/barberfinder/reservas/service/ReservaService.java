package cl.barberfinder.reservas.service;

import cl.barberfinder.reservas.client.CatalogoBarberias;
import cl.barberfinder.reservas.client.CatalogoBarberias.BarberiaInfo;
import cl.barberfinder.reservas.client.CatalogoBarberias.PrecioServicio;
import cl.barberfinder.reservas.dto.ReservaDtos.DisponibilidadResponse;
import cl.barberfinder.reservas.dto.ReservaDtos.HoraDisponible;
import cl.barberfinder.reservas.dto.ReservaDtos.ReprogramarRequest;
import cl.barberfinder.reservas.dto.ReservaDtos.ReservaRequest;
import cl.barberfinder.reservas.dto.ReservaDtos.ReservaResponse;
import cl.barberfinder.reservas.exception.ApiException;
import cl.barberfinder.reservas.model.EstadoReserva;
import cl.barberfinder.reservas.model.Reserva;
import cl.barberfinder.reservas.repository.ReservaRepository;
import cl.barberfinder.reservas.security.AuthUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ReservaService {

    private final ReservaRepository repositorio;
    private final CatalogoBarberias catalogo;
    private final Clock reloj;

    public ReservaService(ReservaRepository repositorio, CatalogoBarberias catalogo, Clock reloj) {
        this.repositorio = repositorio;
        this.catalogo = catalogo;
        this.reloj = reloj;
    }

    @Transactional(readOnly = true)
    public DisponibilidadResponse disponibilidad(Long barberiaId, LocalDate fecha) {
        LocalDateTime ahora = LocalDateTime.now(reloj);
        Set<String> ocupadas = repositorio.findByBarberiaIdAndFechaAndEstadoNot(barberiaId, fecha, EstadoReserva.CANCELADA)
                .stream().map(Reserva::getHora).collect(Collectors.toSet());
        List<HoraDisponible> horas = ReglasReserva.HORAS.stream()
                .map(h -> new HoraDisponible(h, !ocupadas.contains(h)
                        && ReglasReserva.validarHorario(fecha, h, ahora) == null))
                .toList();
        return new DisponibilidadResponse(barberiaId, fecha, horas);
    }

    @Transactional
    public ReservaResponse crear(ReservaRequest req, AuthUser auth) {
        validarHorario(req.fecha(), req.hora());
        PrecioServicio precio = catalogo.precio(req.barberiaId(), req.servicio());
        validarLibre(req.barberiaId(), req.fecha(), req.hora());

        Reserva r = new Reserva();
        r.setClienteId(auth.id());
        r.setBarberiaId(req.barberiaId());
        r.setBarberiaNombre(precio.barberiaNombre());
        r.setServicio(req.servicio());
        r.setPrecio(precio.total());
        r.setFecha(req.fecha());
        r.setHora(req.hora());
        r.setNombreCliente(req.nombreCliente().trim());
        r.setTelefonoCliente(req.telefonoCliente().replace(" ", "").replace("-", ""));
        r.setEstado(EstadoReserva.CONFIRMADA);
        return ReservaResponse.desde(repositorio.save(r));
    }

    @Transactional(readOnly = true)
    public List<ReservaResponse> mias(AuthUser auth, EstadoReserva estado) {
        return repositorio.findByClienteIdOrderByFechaDescHoraDesc(auth.id()).stream()
                .filter(r -> estado == null || r.getEstado() == estado)
                .map(ReservaResponse::desde)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ReservaResponse> todas() {
        return repositorio.findAllByOrderByFechaDescHoraDesc().stream().map(ReservaResponse::desde).toList();
    }

    @Transactional(readOnly = true)
    public List<ReservaResponse> deBarberia(Long barberiaId, LocalDate fecha, AuthUser auth) {
        validarPersonalDeBarberia(barberiaId, auth);
        List<Reserva> reservas = fecha == null
                ? repositorio.findByBarberiaIdOrderByFechaAscHoraAsc(barberiaId)
                : repositorio.findByBarberiaIdAndFechaOrderByHoraAsc(barberiaId, fecha);
        return reservas.stream().map(ReservaResponse::desde).toList();
    }

    @Transactional(readOnly = true)
    public ReservaResponse obtener(Long id, AuthUser auth) {
        Reserva r = buscar(id);
        if (!r.getClienteId().equals(auth.id())) {
            validarPersonalDeBarberia(r.getBarberiaId(), auth);
        }
        return ReservaResponse.desde(r);
    }

    @Transactional
    public ReservaResponse reprogramar(Long id, ReprogramarRequest req, AuthUser auth) {
        Reserva r = buscar(id);
        if (!auth.esAdmin() && !r.getClienteId().equals(auth.id())) {
            throw ApiException.prohibido("Solo puedes reprogramar tus propias reservas.");
        }
        if (r.getEstado() != EstadoReserva.CONFIRMADA) {
            throw ApiException.conflicto("Solo se pueden reprogramar reservas confirmadas.");
        }
        boolean mismoHorario = r.getFecha().equals(req.fecha()) && r.getHora().equals(req.hora());
        if (!mismoHorario) {
            validarHorario(req.fecha(), req.hora());
            validarLibre(r.getBarberiaId(), req.fecha(), req.hora());
            r.setFecha(req.fecha());
            r.setHora(req.hora());
        }
        return ReservaResponse.desde(repositorio.save(r));
    }

    @Transactional
    public ReservaResponse cancelar(Long id, AuthUser auth) {
        Reserva r = buscar(id);
        if (!r.getClienteId().equals(auth.id())) {
            validarPersonalDeBarberia(r.getBarberiaId(), auth);
        }
        if (r.getEstado() != EstadoReserva.CONFIRMADA) {
            throw ApiException.conflicto("Solo se pueden cancelar reservas confirmadas.");
        }
        r.setEstado(EstadoReserva.CANCELADA);
        return ReservaResponse.desde(repositorio.save(r));
    }

    /**
     * Barbero, dueño o admin marcan si el cliente fue atendido o no asistió (o lo devuelven a CONFIRMADA).
     * La hora sigue ocupada en los tres estados; solo una reserva CANCELADA libera la hora.
     */
    @Transactional
    public ReservaResponse cambiarEstado(Long id, EstadoReserva nuevo, AuthUser auth) {
        Reserva r = buscar(id);
        validarPersonalDeBarberia(r.getBarberiaId(), auth);
        if (nuevo == EstadoReserva.CANCELADA) {
            throw ApiException.solicitudInvalida("Para cancelar usa PATCH /api/reservas/{id}/cancelar.");
        }
        if (r.getEstado() == EstadoReserva.CANCELADA) {
            throw ApiException.conflicto("La reserva está cancelada y no se puede modificar.");
        }
        r.setEstado(nuevo);
        return ReservaResponse.desde(repositorio.save(r));
    }

    @Transactional
    public void eliminar(Long id) {
        repositorio.delete(buscar(id));
    }

    /**
     * ADMIN siempre puede. BARBERO solo si trabaja en esa barbería (claim barberiaId del token).
     * DUENO solo si la barbería es suya (se consulta a ms-barberias).
     */
    void validarPersonalDeBarberia(Long barberiaId, AuthUser auth) {
        if (auth.esAdmin()) {
            return;
        }
        if ("BARBERO".equals(auth.rol()) && barberiaId.equals(auth.barberiaId())) {
            return;
        }
        if ("DUENO".equals(auth.rol())) {
            BarberiaInfo info = catalogo.barberia(barberiaId);
            if (auth.id().equals(info.duenoId())) {
                return;
            }
        }
        throw ApiException.prohibido("No tienes acceso a las reservas de esta barbería.");
    }

    private void validarHorario(LocalDate fecha, String hora) {
        String error = ReglasReserva.validarHorario(fecha, hora, LocalDateTime.now(reloj));
        if (error != null) {
            throw ApiException.solicitudInvalida(error);
        }
    }

    private void validarLibre(Long barberiaId, LocalDate fecha, String hora) {
        if (repositorio.existsByBarberiaIdAndFechaAndHoraAndEstadoNot(barberiaId, fecha, hora, EstadoReserva.CANCELADA)) {
            throw ApiException.conflicto("Esa hora ya está reservada en esta barbería.");
        }
    }

    private Reserva buscar(Long id) {
        return repositorio.findById(id)
                .orElseThrow(() -> ApiException.noEncontrado("Reserva no encontrada."));
    }
}
