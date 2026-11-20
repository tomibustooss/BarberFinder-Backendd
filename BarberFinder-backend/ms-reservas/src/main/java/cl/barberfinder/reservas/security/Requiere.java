package cl.barberfinder.reservas.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marca un endpoint como protegido.
 * Sin roles: basta con estar autenticado. Con roles: el usuario debe tener alguno de ellos.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Requiere {
    String[] roles() default {};
}
