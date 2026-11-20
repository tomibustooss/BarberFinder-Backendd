package cl.barberfinder.reservas.repository;

import cl.barberfinder.reservas.model.EstadoReserva;
import cl.barberfinder.reservas.model.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    boolean existsByBarberiaIdAndFechaAndHoraAndEstadoNot(Long barberiaId, LocalDate fecha, String hora, EstadoReserva estado);

    List<Reserva> findByBarberiaIdAndFechaAndEstadoNot(Long barberiaId, LocalDate fecha, EstadoReserva estado);

    List<Reserva> findByClienteIdOrderByFechaDescHoraDesc(Long clienteId);

    List<Reserva> findByBarberiaIdOrderByFechaAscHoraAsc(Long barberiaId);

    List<Reserva> findByBarberiaIdAndFechaOrderByHoraAsc(Long barberiaId, LocalDate fecha);

    List<Reserva> findAllByOrderByFechaDescHoraDesc();
}
