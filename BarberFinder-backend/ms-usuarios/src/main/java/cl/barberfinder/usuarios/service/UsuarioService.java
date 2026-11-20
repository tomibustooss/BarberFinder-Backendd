package cl.barberfinder.usuarios.service;

import cl.barberfinder.usuarios.dto.UsuarioDtos.ActualizarPerfilRequest;
import cl.barberfinder.usuarios.dto.UsuarioDtos.AuthResponse;
import cl.barberfinder.usuarios.dto.UsuarioDtos.CambiarPasswordRequest;
import cl.barberfinder.usuarios.dto.UsuarioDtos.CambiarRolRequest;
import cl.barberfinder.usuarios.dto.UsuarioDtos.CrearUsuarioRequest;
import cl.barberfinder.usuarios.dto.UsuarioDtos.LoginRequest;
import cl.barberfinder.usuarios.dto.UsuarioDtos.RecuperarResponse;
import cl.barberfinder.usuarios.dto.UsuarioDtos.RegistroRequest;
import cl.barberfinder.usuarios.dto.UsuarioDtos.RestablecerRequest;
import cl.barberfinder.usuarios.dto.UsuarioDtos.UsuarioResponse;
import cl.barberfinder.usuarios.exception.ApiException;
import cl.barberfinder.usuarios.model.Rol;
import cl.barberfinder.usuarios.model.Usuario;
import cl.barberfinder.usuarios.repository.UsuarioRepository;
import cl.barberfinder.usuarios.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class UsuarioService {

    private static final Logger log = LoggerFactory.getLogger(UsuarioService.class);
    private static final int MINUTOS_CODIGO = 15;
    private static final int MAX_INTENTOS_CODIGO = 5;
    private static final String MENSAJE_RECUPERACION =
            "Si el correo está registrado, se generó un código de recuperación válido por 15 minutos.";

    private final UsuarioRepository repositorio;
    private final PasswordEncoder encoder;
    private final JwtService jwtService;
    private final boolean modoDemoRecuperacion;
    private final SecureRandom random = new SecureRandom();

    public UsuarioService(UsuarioRepository repositorio,
                          PasswordEncoder encoder,
                          JwtService jwtService,
                          @Value("${app.recovery.demo-mode:true}") boolean modoDemoRecuperacion) {
        this.repositorio = repositorio;
        this.encoder = encoder;
        this.jwtService = jwtService;
        this.modoDemoRecuperacion = modoDemoRecuperacion;
    }

    @Transactional
    public AuthResponse registrar(RegistroRequest req) {
        Usuario nuevo = crear(req.nombre(), req.email(), req.telefono(), req.password(), Rol.CLIENTE, null);
        return autenticar(nuevo);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest req) {
        Usuario usuario = repositorio.findByEmailIgnoreCase(normalizarEmail(req.email()))
                .orElseThrow(() -> ApiException.noAutorizado("Correo o contraseña incorrectos."));
        if (!encoder.matches(req.password(), usuario.getPasswordHash())) {
            throw ApiException.noAutorizado("Correo o contraseña incorrectos.");
        }
        if (!usuario.isActivo()) {
            throw ApiException.prohibido("La cuenta está desactivada. Contacta al administrador.");
        }
        return autenticar(usuario);
    }

    @Transactional
    public RecuperarResponse solicitarRecuperacion(String email) {
        var encontrado = repositorio.findByEmailIgnoreCase(normalizarEmail(email));
        if (encontrado.isEmpty()) {
            return new RecuperarResponse(MENSAJE_RECUPERACION, null, MINUTOS_CODIGO);
        }
        Usuario usuario = encontrado.get();
        String codigo = String.format("%06d", random.nextInt(1_000_000));
        usuario.setCodigoRecuperacionHash(encoder.encode(codigo));
        usuario.setCodigoExpiraEn(LocalDateTime.now().plusMinutes(MINUTOS_CODIGO));
        usuario.setIntentosRecuperacion(0);
        repositorio.save(usuario);

        if (modoDemoRecuperacion) {
            log.info("[DEMO] Código de recuperación para {}: {}", usuario.getEmail(), codigo);
            return new RecuperarResponse(MENSAJE_RECUPERACION, codigo, MINUTOS_CODIGO);
        }
        return new RecuperarResponse(MENSAJE_RECUPERACION, null, MINUTOS_CODIGO);
    }

    @Transactional(noRollbackFor = ApiException.class)
    public void restablecerPassword(RestablecerRequest req) {
        Usuario usuario = repositorio.findByEmailIgnoreCase(normalizarEmail(req.email()))
                .orElseThrow(() -> ApiException.solicitudInvalida("El código no es válido o expiró."));

        if (usuario.getCodigoRecuperacionHash() == null || usuario.getCodigoExpiraEn() == null
                || usuario.getCodigoExpiraEn().isBefore(LocalDateTime.now())) {
            throw ApiException.solicitudInvalida("El código no es válido o expiró.");
        }
        if (usuario.getIntentosRecuperacion() >= MAX_INTENTOS_CODIGO) {
            limpiarCodigo(usuario);
            repositorio.save(usuario);
            throw ApiException.solicitudInvalida("Demasiados intentos. Solicita un código nuevo.");
        }
        if (!encoder.matches(req.codigo(), usuario.getCodigoRecuperacionHash())) {
            usuario.setIntentosRecuperacion(usuario.getIntentosRecuperacion() + 1);
            repositorio.save(usuario);
            throw ApiException.solicitudInvalida("El código no es válido o expiró.");
        }
        usuario.setPasswordHash(encoder.encode(req.nuevaPassword()));
        limpiarCodigo(usuario);
        repositorio.save(usuario);
    }

    @Transactional(readOnly = true)
    public UsuarioResponse obtener(Long id) {
        return UsuarioResponse.desde(buscar(id));
    }

    @Transactional(readOnly = true)
    public List<UsuarioResponse> listar(Rol rol) {
        List<Usuario> usuarios = rol == null ? repositorio.findAll() : repositorio.findByRol(rol);
        return usuarios.stream().map(UsuarioResponse::desde).toList();
    }

    @Transactional
    public UsuarioResponse actualizarPerfil(Long id, ActualizarPerfilRequest req) {
        Usuario usuario = buscar(id);
        usuario.setNombre(req.nombre().trim());
        usuario.setTelefono(normalizarTelefono(req.telefono()));
        return UsuarioResponse.desde(repositorio.save(usuario));
    }

    @Transactional
    public void cambiarPassword(Long id, CambiarPasswordRequest req) {
        Usuario usuario = buscar(id);
        if (!encoder.matches(req.passwordActual(), usuario.getPasswordHash())) {
            throw ApiException.solicitudInvalida("La contraseña actual no es correcta.");
        }
        if (encoder.matches(req.nuevaPassword(), usuario.getPasswordHash())) {
            throw ApiException.solicitudInvalida("La nueva contraseña debe ser distinta a la actual.");
        }
        usuario.setPasswordHash(encoder.encode(req.nuevaPassword()));
        repositorio.save(usuario);
    }

    @Transactional
    public UsuarioResponse crearComoAdmin(CrearUsuarioRequest req) {
        Usuario nuevo = crear(req.nombre(), req.email(), req.telefono(), req.password(), req.rol(), req.barberiaId());
        return UsuarioResponse.desde(nuevo);
    }

    @Transactional
    public UsuarioResponse cambiarRol(Long id, CambiarRolRequest req, Long adminId) {
        Usuario usuario = buscar(id);
        if (usuario.getId().equals(adminId) && req.rol() != Rol.ADMIN) {
            throw ApiException.solicitudInvalida("No puedes quitarte el rol de administrador a ti mismo.");
        }
        validarBarberia(req.rol(), req.barberiaId());
        usuario.setRol(req.rol());
        usuario.setBarberiaId(req.rol() == Rol.BARBERO ? req.barberiaId() : null);
        return UsuarioResponse.desde(repositorio.save(usuario));
    }

    @Transactional
    public UsuarioResponse cambiarEstado(Long id, boolean activo, Long adminId) {
        Usuario usuario = buscar(id);
        if (usuario.getId().equals(adminId) && !activo) {
            throw ApiException.solicitudInvalida("No puedes desactivar tu propia cuenta.");
        }
        usuario.setActivo(activo);
        return UsuarioResponse.desde(repositorio.save(usuario));
    }

    @Transactional
    public void eliminar(Long id, Long adminId) {
        Usuario usuario = buscar(id);
        if (usuario.getId().equals(adminId)) {
            throw ApiException.solicitudInvalida("No puedes eliminar tu propia cuenta.");
        }
        repositorio.delete(usuario);
    }

    Usuario crear(String nombre, String email, String telefono, String password, Rol rol, Long barberiaId) {
        String correo = normalizarEmail(email);
        if (repositorio.existsByEmailIgnoreCase(correo)) {
            throw ApiException.conflicto("Ya existe una cuenta con ese correo.");
        }
        validarBarberia(rol, barberiaId);
        Usuario u = new Usuario();
        u.setNombre(nombre.trim());
        u.setEmail(correo);
        u.setTelefono(normalizarTelefono(telefono));
        u.setPasswordHash(encoder.encode(password));
        u.setRol(rol);
        u.setBarberiaId(rol == Rol.BARBERO ? barberiaId : null);
        u.setActivo(true);
        return repositorio.save(u);
    }

    private void validarBarberia(Rol rol, Long barberiaId) {
        if (rol == Rol.BARBERO && barberiaId == null) {
            throw ApiException.solicitudInvalida("Un barbero debe tener asignada una barbería (barberiaId).");
        }
    }

    private AuthResponse autenticar(Usuario usuario) {
        return new AuthResponse(jwtService.generar(usuario), "Bearer", jwtService.getExpiraMinutos(),
                UsuarioResponse.desde(usuario));
    }

    private Usuario buscar(Long id) {
        return repositorio.findById(id)
                .orElseThrow(() -> ApiException.noEncontrado("Usuario no encontrado."));
    }

    private void limpiarCodigo(Usuario usuario) {
        usuario.setCodigoRecuperacionHash(null);
        usuario.setCodigoExpiraEn(null);
        usuario.setIntentosRecuperacion(0);
    }

    static String normalizarEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    static String normalizarTelefono(String telefono) {
        return telefono == null ? null : telefono.replace(" ", "").replace("-", "");
    }
}
