package cl.barberfinder.usuarios.repository;

import cl.barberfinder.usuarios.model.Rol;
import cl.barberfinder.usuarios.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    List<Usuario> findByRol(Rol rol);

    long countByRol(Rol rol);
}
