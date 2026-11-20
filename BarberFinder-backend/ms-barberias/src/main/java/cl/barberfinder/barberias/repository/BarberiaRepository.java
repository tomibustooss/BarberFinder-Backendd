package cl.barberfinder.barberias.repository;

import cl.barberfinder.barberias.model.Barberia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BarberiaRepository extends JpaRepository<Barberia, Long> {

    List<Barberia> findByActivaTrueOrderByIdAsc();

    List<Barberia> findByDuenoIdOrderByIdAsc(Long duenoId);
}
